package com.lacivita.turnos.notifications.application;

import com.lacivita.turnos.notifications.application.MessageComposer.Ready;
import com.lacivita.turnos.notifications.application.MessageComposer.Skip;
import com.lacivita.turnos.notifications.domain.ChannelKind;
import com.lacivita.turnos.notifications.domain.DeliveryFailedException;
import com.lacivita.turnos.notifications.domain.DeliveryStatus;
import com.lacivita.turnos.notifications.domain.Notification;
import com.lacivita.turnos.notifications.domain.NotificationChannel;
import com.lacivita.turnos.notifications.domain.NotificationRepository;
import com.lacivita.turnos.notifications.domain.OutgoingMessage;
import com.lacivita.turnos.shared.tenancy.TenantContext;
import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Envía una notificación ya tomada por el despachador, dentro del negocio al que pertenece:
 *
 * <ol>
 *   <li>Arma el mensaje con los datos actuales del turno (en una transacción corta).
 *   <li>Lo envía por su canal, fuera de toda transacción: un proveedor lento no retiene conexiones.
 *   <li>Registra el resultado: enviado, o un intento fallido que se reintenta más tarde.
 * </ol>
 */
@Component
class NotificationDelivery {

    private static final Logger log = LoggerFactory.getLogger(NotificationDelivery.class);

    private final NotificationRepository notifications;
    private final MessageComposer composer;
    private final Map<ChannelKind, NotificationChannel> channels;
    private final TransactionTemplate transactions;
    private final Clock clock;

    NotificationDelivery(
            NotificationRepository notifications,
            MessageComposer composer,
            List<NotificationChannel> channels,
            PlatformTransactionManager transactionManager,
            Clock clock) {
        this.notifications = notifications;
        this.composer = composer;
        // Spring entrega la lista ordenada por @Order: si hay dos canales del mismo tipo (las pruebas
        // reemplazan el email), rige el de mayor prioridad.
        this.channels = new EnumMap<>(ChannelKind.class);
        channels.forEach(channel -> this.channels.putIfAbsent(channel.kind(), channel));
        this.transactions = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    void deliver(UUID businessId, UUID notificationId) {
        TenantContext.callInBusiness(businessId, () -> {
            deliverInBusiness(notificationId);
            return null;
        });
    }

    private void deliverInBusiness(UUID notificationId) {
        Optional<Prepared> prepared;
        try {
            prepared = transactions.execute(status -> prepare(notificationId, clock.instant()));
        } catch (RuntimeException ex) {
            // Un error al armar el mensaje cuenta como intento: si no, se reintentaría para siempre.
            log.error("No se pudo armar la notificación {}", notificationId, ex);
            record(notificationId, "compose_error");
            return;
        }
        if (prepared == null || prepared.isEmpty()) {
            return;
        }
        record(notificationId, send(prepared.get()));
    }

    /** El mensaje listo, o vacío si ya no está para enviar o se descartó. */
    private Optional<Prepared> prepare(UUID notificationId, Instant now) {
        var found = notifications.findById(notificationId).filter(notification -> notification.isClaimed(now));
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Notification notification = found.get();
        return switch (composer.compose(notification, now)) {
            case Ready(OutgoingMessage message) -> Optional.of(new Prepared(notification.getChannel(), message));
            case Skip(String reason) -> {
                notification.skip(reason, now);
                yield Optional.empty();
            }
        };
    }

    /** @return {@code null} si salió; si no, el motivo del fallo */
    private String send(Prepared prepared) {
        var channel = channels.get(prepared.channel());
        if (channel == null) {
            return "channel_unavailable";
        }
        try {
            channel.send(prepared.message());
            return null;
        } catch (DeliveryFailedException ex) {
            log.warn("No salió una notificación por {}: {}", prepared.channel(), ex.reason(), ex);
            return ex.reason();
        } catch (RuntimeException ex) {
            log.error("Error inesperado al enviar una notificación por {}", prepared.channel(), ex);
            return "unexpected_error";
        }
    }

    private void record(UUID notificationId, String failure) {
        transactions.executeWithoutResult(status -> {
            var now = clock.instant();
            // Aunque el envío haya tardado más que el plazo tomado, el resultado se registra: así no se
            // envía dos veces lo que ya salió.
            notifications
                    .findById(notificationId)
                    .filter(notification -> notification.getStatus() == DeliveryStatus.PENDING)
                    .ifPresent(notification -> {
                        if (failure == null) {
                            notification.markSent(now);
                        } else {
                            notification.markFailed(failure, now);
                        }
                    });
        });
    }

    private record Prepared(ChannelKind channel, OutgoingMessage message) {}
}
