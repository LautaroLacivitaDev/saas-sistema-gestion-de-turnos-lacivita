package com.lacivita.turnos.notifications.application;

import com.lacivita.turnos.notifications.domain.Notification;
import com.lacivita.turnos.notifications.domain.NotificationRepository;
import com.lacivita.turnos.shared.tenancy.TenantContext;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.jobrunr.jobs.annotations.Recurring;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Envía las notificaciones que ya tienen que salir, de todos los negocios. Corre cada minuto con JobRunr
 * (recordatorios, resúmenes y reintentos) y, además, apenas se confirma un cambio en un turno, para que el
 * cliente reciba su confirmación en segundos.
 *
 * <p>Varios servidores pueden correrlo a la vez: cada uno toma notificaciones distintas (ver {@link
 * NotificationRepository#findDueForUpdate}). Es pública solo porque JobRunr la invoca por reflexión.
 */
@Component
public class NotificationDispatcher {

    static final int BATCH_SIZE = 20;

    /** Tope por corrida, para que una corrida no se extienda indefinidamente; lo que quede sale en la próxima. */
    static final int MAX_BATCHES = 50;

    private final NotificationRepository notifications;
    private final NotificationDelivery delivery;
    private final NotificationProperties properties;
    private final TaskExecutor executor;
    private final TransactionTemplate transactions;
    private final Clock clock;

    NotificationDispatcher(
            NotificationRepository notifications,
            NotificationDelivery delivery,
            NotificationProperties properties,
            @Qualifier("applicationTaskExecutor") TaskExecutor executor,
            PlatformTransactionManager transactionManager,
            Clock clock) {
        this.notifications = notifications;
        this.delivery = delivery;
        this.properties = properties;
        this.executor = executor;
        this.transactions = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    @Recurring(id = "notifications-dispatch", interval = "PT1M")
    public void dispatchDue() {
        for (int batch = 0; batch < MAX_BATCHES; batch++) {
            var claimed = claimBatch();
            claimed.forEach(notification -> delivery.deliver(notification.businessId(), notification.id()));
            if (claimed.size() < BATCH_SIZE) {
                return;
            }
        }
    }

    /** Envía lo pendiente apenas se confirme la transacción actual (en segundo plano). */
    void dispatchAfterCommit() {
        if (!properties.sendOnCommit()) {
            return;
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executor.execute(NotificationDispatcher.this::dispatchDue);
                }
            });
        } else {
            executor.execute(this::dispatchDue);
        }
    }

    /** Toma un lote de lo que hay que enviar, de cualquier negocio, en una transacción corta. */
    private List<Claimed> claimBatch() {
        return TenantContext.callAsSystem(
                "enviar las notificaciones pendientes de todos los negocios",
                () -> transactions.execute(status -> {
                    var now = clock.instant();
                    var due = notifications.findDueForUpdate(now, Limit.of(BATCH_SIZE));
                    due.forEach(notification -> notification.claim(now));
                    return due.stream().map(Claimed::of).toList();
                }));
    }

    private record Claimed(UUID businessId, UUID id) {

        static Claimed of(Notification notification) {
            return new Claimed(notification.getBusinessId(), notification.getId());
        }
    }
}
