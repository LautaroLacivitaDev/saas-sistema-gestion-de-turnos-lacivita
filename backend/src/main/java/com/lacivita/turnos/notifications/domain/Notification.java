package com.lacivita.turnos.notifications.domain;

import com.lacivita.turnos.shared.domain.Ids;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

/**
 * Aviso pendiente o enviado: es a la vez la bandeja de salida (outbox) y el registro de envíos.
 *
 * <p>Se guarda en la misma transacción que el cambio del turno, así nunca se pierde: si el envío falla,
 * se reintenta más tarde. Un recordatorio se guarda con la hora en que corresponde enviarlo; si el turno
 * cambia, se cancela y se crea otro.
 */
// DECISIÓN: 5 intentos (al momento y a los 1, 5, 15 y 60 minutos). Después queda FAILED en el registro
// del turno, para que el local avise por otro medio.
@Entity
@Table(name = "notification")
public class Notification {

    public static final int MAX_ATTEMPTS = 5;

    /** Espera antes de cada reintento: el primero, al minuto; el último, a la hora. */
    private static final List<Duration> BACKOFF =
            List.of(Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(15), Duration.ofHours(1));

    /** Cuánto tiempo queda tomado un envío en curso antes de que otro servidor pueda reintentarlo. */
    public static final Duration LEASE = Duration.ofMinutes(2);

    @Id
    private UUID id;

    @TenantId
    private UUID businessId;

    /** Nulo solo en el resumen de agenda, que abarca todos los turnos del día. */
    private UUID appointmentId;

    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    private Audience audience;

    @Enumerated(EnumType.STRING)
    private ChannelKind channel;

    /** Profesional que recibe el aviso; nulo si es para el cliente. */
    private UUID recipientUserId;

    /** Día del resumen de agenda; nulo en los demás avisos. */
    private LocalDate agendaDate;

    private Instant dueAt;

    private Instant nextAttemptAt;

    @Enumerated(EnumType.STRING)
    private DeliveryStatus status;

    private int attempts;

    private Instant lockedUntil;

    private Instant sentAt;

    private String lastError;

    private Instant createdAt;

    private Instant updatedAt;

    @Version
    private Long version;

    protected Notification() {
        // Requerido por JPA.
    }

    private Notification(
            UUID businessId,
            UUID appointmentId,
            NotificationType type,
            Audience audience,
            UUID recipientUserId,
            Instant dueAt,
            Instant now) {
        this.id = Ids.newId();
        this.businessId = Objects.requireNonNull(businessId, "businessId");
        this.appointmentId = Objects.requireNonNull(appointmentId, "appointmentId");
        this.type = Objects.requireNonNull(type, "type");
        this.audience = audience;
        this.channel = ChannelKind.EMAIL;
        this.recipientUserId = recipientUserId;
        this.dueAt = Objects.requireNonNull(dueAt, "dueAt");
        this.nextAttemptAt = dueAt;
        this.status = DeliveryStatus.PENDING;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Aviso al cliente sobre su turno. */
    public static Notification toCustomer(
            UUID businessId, UUID appointmentId, NotificationType type, Instant dueAt, Instant now) {
        if (type == NotificationType.DAILY_AGENDA) {
            throw new IllegalArgumentException("El resumen de agenda es para los profesionales");
        }
        return new Notification(businessId, appointmentId, type, Audience.CUSTOMER, null, dueAt, now);
    }

    /** Aviso a un profesional sobre uno de sus turnos. */
    public static Notification toBarber(
            UUID businessId, UUID appointmentId, UUID barberId, NotificationType type, Instant now) {
        if (type == NotificationType.DAILY_AGENDA || type == NotificationType.APPOINTMENT_REMINDER) {
            throw new IllegalArgumentException("Al profesional se le avisa de los cambios en sus turnos");
        }
        Objects.requireNonNull(barberId, "barberId");
        return new Notification(businessId, appointmentId, type, Audience.BARBER, barberId, now, now);
    }

    /** {@code true} si le toca salir: está pendiente, llegó su hora y nadie lo está enviando. */
    public boolean isDue(Instant now) {
        return status == DeliveryStatus.PENDING
                && !nextAttemptAt.isAfter(now)
                && (lockedUntil == null || !lockedUntil.isAfter(now));
    }

    /** Lo toma un servidor para enviarlo. Mientras dura el plazo, ningún otro lo toma. */
    public void claim(Instant now) {
        if (!isDue(now)) {
            throw new IllegalStateException("La notificación no está para enviar");
        }
        this.lockedUntil = now.plus(LEASE);
        this.updatedAt = now;
    }

    /** {@code true} si este servidor lo tomó y todavía no se registró el resultado. */
    public boolean isClaimed(Instant now) {
        return status == DeliveryStatus.PENDING && lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void markSent(Instant now) {
        requirePending();
        this.attempts++;
        this.status = DeliveryStatus.SENT;
        this.sentAt = now;
        this.lockedUntil = null;
        this.lastError = null;
        this.updatedAt = now;
    }

    /** El envío falló: se reintenta más tarde o, si no quedan intentos, queda FAILED. */
    public void markFailed(String error, Instant now) {
        requirePending();
        this.attempts++;
        this.lastError = shorten(error);
        this.lockedUntil = null;
        this.updatedAt = now;
        if (attempts >= MAX_ATTEMPTS) {
            this.status = DeliveryStatus.FAILED;
        } else {
            this.nextAttemptAt = now.plus(BACKOFF.get(attempts - 1));
        }
    }

    /** No tiene sentido enviarlo ni reintentarlo (por ejemplo, no hay email al que mandarlo). */
    public void skip(String reason, Instant now) {
        requirePending();
        this.status = DeliveryStatus.SKIPPED;
        this.lastError = shorten(reason);
        this.lockedUntil = null;
        this.updatedAt = now;
    }

    /** Deja de corresponder antes de salir, por ejemplo un recordatorio de un turno que se movió. */
    public void cancel(Instant now) {
        if (status == DeliveryStatus.PENDING) {
            this.status = DeliveryStatus.CANCELLED;
            this.lockedUntil = null;
            this.updatedAt = now;
        }
    }

    public UUID getId() {
        return id;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public Optional<UUID> appointmentId() {
        return Optional.ofNullable(appointmentId);
    }

    public NotificationType getType() {
        return type;
    }

    public Audience getAudience() {
        return audience;
    }

    public ChannelKind getChannel() {
        return channel;
    }

    public Optional<UUID> recipientUserId() {
        return Optional.ofNullable(recipientUserId);
    }

    public Optional<LocalDate> agendaDate() {
        return Optional.ofNullable(agendaDate);
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public Optional<Instant> sentAt() {
        return Optional.ofNullable(sentAt);
    }

    public Optional<String> lastError() {
        return Optional.ofNullable(lastError);
    }

    private void requirePending() {
        if (status != DeliveryStatus.PENDING) {
            throw new IllegalStateException("La notificación ya no está pendiente: " + status);
        }
    }

    private static String shorten(String text) {
        if (text == null) {
            return null;
        }
        return text.length() <= 200 ? text : text.substring(0, 200);
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || (other instanceof Notification notification && id != null && id.equals(notification.id));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
