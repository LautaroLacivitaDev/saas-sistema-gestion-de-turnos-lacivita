package com.lacivita.turnos.notifications.domain;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends Repository<Notification, UUID> {

    Notification save(Notification notification);

    Optional<Notification> findById(UUID id);

    /** Registro de envíos de un turno, del más viejo al más nuevo. */
    @Query("select n from Notification n where n.appointmentId = :appointmentId order by n.createdAt, n.dueAt")
    List<Notification> findByAppointment(@Param("appointmentId") UUID appointmentId);

    @Query("""
            select n from Notification n
            where n.appointmentId = :appointmentId
              and n.type = com.lacivita.turnos.notifications.domain.NotificationType.APPOINTMENT_REMINDER
              and n.status = com.lacivita.turnos.notifications.domain.DeliveryStatus.PENDING
            """)
    List<Notification> findPendingReminders(@Param("appointmentId") UUID appointmentId);

    /**
     * Lo que hay que enviar ahora, bloqueado para esta transacción. Lo que ya bloqueó otro servidor se saltea
     * ({@code SKIP LOCKED}), así varios servidores reparten el trabajo sin enviar dos veces lo mismo.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("""
            select n from Notification n
            where n.status = com.lacivita.turnos.notifications.domain.DeliveryStatus.PENDING
              and n.nextAttemptAt <= :now
              and (n.lockedUntil is null or n.lockedUntil <= :now)
            order by n.nextAttemptAt
            """)
    List<Notification> findDueForUpdate(@Param("now") Instant now, Limit limit);

    /**
     * Programa el resumen de agenda del profesional para ese día, si todavía no estaba. No falla si dos
     * reservas lo programan a la vez: la reserva nunca debe caerse por un aviso.
     */
    @Modifying
    @Query(nativeQuery = true, value = """
            INSERT INTO notification (id, business_id, type, audience, channel, recipient_user_id, agenda_date,
                                      due_at, next_attempt_at, status, attempts, created_at, updated_at, version)
            VALUES (:id, :businessId, 'DAILY_AGENDA', 'BARBER', 'EMAIL', :barberId, :agendaDate,
                    :dueAt, :dueAt, 'PENDING', 0, :now, :now, 0)
            ON CONFLICT (business_id, recipient_user_id, agenda_date) WHERE type = 'DAILY_AGENDA' DO NOTHING
            """)
    int scheduleDailyAgenda(
            @Param("id") UUID id,
            @Param("businessId") UUID businessId,
            @Param("barberId") UUID barberId,
            @Param("agendaDate") LocalDate agendaDate,
            @Param("dueAt") Instant dueAt,
            @Param("now") Instant now);
}
