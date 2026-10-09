package com.lacivita.turnos.notifications.application;

import com.lacivita.turnos.booking.AppointmentBooked;
import com.lacivita.turnos.booking.AppointmentDetails;
import com.lacivita.turnos.booking.AppointmentDirectory;
import com.lacivita.turnos.booking.AppointmentRescheduled;
import com.lacivita.turnos.booking.AppointmentStatusChanged;
import com.lacivita.turnos.notifications.domain.AppNotice;
import com.lacivita.turnos.notifications.domain.AppNoticeRepository;
import com.lacivita.turnos.notifications.domain.Notification;
import com.lacivita.turnos.notifications.domain.NotificationRepository;
import com.lacivita.turnos.notifications.domain.NotificationSettingsRepository;
import com.lacivita.turnos.notifications.domain.NotificationType;
import com.lacivita.turnos.shared.domain.Ids;
import com.lacivita.turnos.shared.tenancy.TenantContext;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Decide qué avisar cuando cambia un turno y lo deja en la bandeja de salida, en la misma transacción que el
 * cambio: si el turno se guarda, el aviso también; si el cambio se revierte, no queda nada que enviar.
 *
 * <table>
 *   <caption>Avisos</caption>
 *   <tr><th>Cambio</th><th>Cliente</th><th>Profesional</th></tr>
 *   <tr><td>Turno reservado</td><td>Email con .ics y recordatorios</td><td>Aviso en la app, email y
 *       resumen del día</td></tr>
 *   <tr><td>Turno movido</td><td>Email con .ics y recordatorios nuevos</td><td>Aviso en la app y email
 *       (también a quien lo tenía antes)</td></tr>
 *   <tr><td>Turno cancelado</td><td>Email con .ics</td><td>Aviso en la app y email</td></tr>
 * </table>
 *
 * <p>A quien hizo el cambio no se le avisa: si un barbero carga su propio turno, ya lo sabe.
 */
// DECISIÓN: no se implementan los avisos "opcionales" al gerente y al dueño (resumen de turnos nuevos y
// cancelaciones). La especificación los marca como opcionales y el panel ya muestra la agenda completa.
// DECISIÓN: el resumen diario sale a las 7:00 de la sucursal del primer turno del día del profesional.
@Component
class AppointmentNotifier {

    static final LocalTime DAILY_AGENDA_AT = LocalTime.of(7, 0);

    private final AppointmentDirectory appointments;
    private final NotificationRepository notifications;
    private final AppNoticeRepository notices;
    private final NotificationSettingsRepository settings;
    private final NotificationDispatcher dispatcher;
    private final Clock clock;

    AppointmentNotifier(
            AppointmentDirectory appointments,
            NotificationRepository notifications,
            AppNoticeRepository notices,
            NotificationSettingsRepository settings,
            NotificationDispatcher dispatcher,
            Clock clock) {
        this.appointments = appointments;
        this.notifications = notifications;
        this.notices = notices;
        this.settings = settings;
        this.dispatcher = dispatcher;
        this.clock = clock;
    }

    @EventListener
    void on(AppointmentBooked event) {
        var now = clock.instant();
        appointments.details(event.businessId(), event.appointmentId()).ifPresent(appointment -> {
            toCustomer(appointment, NotificationType.APPOINTMENT_BOOKED, now);
            scheduleReminders(appointment, now);
            if (!isTheActor(appointment.barberId())) {
                toBarber(
                        appointment,
                        appointment.barberId(),
                        NotificationType.APPOINTMENT_BOOKED,
                        NotificationTexts.bookedNotice(appointment),
                        now);
            }
            scheduleDailyAgenda(appointment, now);
            dispatcher.dispatchAfterCommit();
        });
    }

    @EventListener
    void on(AppointmentRescheduled event) {
        var now = clock.instant();
        appointments.details(event.businessId(), event.appointmentId()).ifPresent(appointment -> {
            cancelPendingReminders(appointment.id(), now);
            toCustomer(appointment, NotificationType.APPOINTMENT_RESCHEDULED, now);
            scheduleReminders(appointment, now);
            if (!isTheActor(event.barberAfter())) {
                toBarber(
                        appointment,
                        event.barberAfter(),
                        NotificationType.APPOINTMENT_RESCHEDULED,
                        NotificationTexts.movedNotice(appointment),
                        now);
            }
            if (event.changedBarber() && !isTheActor(event.barberBefore())) {
                toBarber(
                        appointment,
                        event.barberBefore(),
                        NotificationType.APPOINTMENT_RESCHEDULED,
                        NotificationTexts.reassignedNotice(appointment, event.startBefore()),
                        now);
            }
            scheduleDailyAgenda(appointment, now);
            dispatcher.dispatchAfterCommit();
        });
    }

    @EventListener
    void on(AppointmentStatusChanged event) {
        var now = clock.instant();
        if (event.leftTheUpcomingAgenda()) {
            cancelPendingReminders(event.appointmentId(), now);
        }
        if (!event.isCancellation()) {
            return;
        }
        appointments.details(event.businessId(), event.appointmentId()).ifPresent(appointment -> {
            toCustomer(appointment, NotificationType.APPOINTMENT_CANCELLED, now);
            if (!isTheActor(appointment.barberId())) {
                toBarber(
                        appointment,
                        appointment.barberId(),
                        NotificationType.APPOINTMENT_CANCELLED,
                        NotificationTexts.cancelledNotice(appointment),
                        now);
            }
            dispatcher.dispatchAfterCommit();
        });
    }

    /** Si el cliente no dejó email, el aviso queda en el registro como omitido al intentar enviarlo. */
    private void toCustomer(AppointmentDetails appointment, NotificationType type, Instant now) {
        notifications.save(Notification.toCustomer(appointment.businessId(), appointment.id(), type, now, now));
    }

    private void toBarber(
            AppointmentDetails appointment, UUID barberId, NotificationType type, String notice, Instant now) {
        notices.save(AppNotice.of(appointment.businessId(), barberId, appointment.id(), type, notice, now));
        notifications.save(Notification.toBarber(appointment.businessId(), appointment.id(), barberId, type, now));
    }

    private void scheduleReminders(AppointmentDetails appointment, Instant now) {
        if (!appointment.isUpcoming()) {
            return;
        }
        settings.remindersOf(appointment.businessId())
                .sendTimes(appointment.startsAt(), now)
                .forEach(time -> notifications.save(Notification.toCustomer(
                        appointment.businessId(), appointment.id(), NotificationType.APPOINTMENT_REMINDER, time, now)));
    }

    private void cancelPendingReminders(UUID appointmentId, Instant now) {
        notifications.findPendingReminders(appointmentId).forEach(reminder -> reminder.cancel(now));
    }

    /** El resumen del día del profesional, si todavía no salió y no estaba programado. */
    private void scheduleDailyAgenda(AppointmentDetails appointment, Instant now) {
        if (!appointment.isUpcoming()) {
            return;
        }
        var zone = appointment.timeZone();
        var day = LocalDate.ofInstant(appointment.startsAt(), zone);
        var dueAt = day.atTime(DAILY_AGENDA_AT).atZone(zone).toInstant();
        if (dueAt.isAfter(now)) {
            notifications.scheduleDailyAgenda(
                    Ids.newId(), appointment.businessId(), appointment.barberId(), day, dueAt, now);
        }
    }

    private static boolean isTheActor(UUID userId) {
        return TenantContext.currentUser().map(userId::equals).orElse(false);
    }
}
