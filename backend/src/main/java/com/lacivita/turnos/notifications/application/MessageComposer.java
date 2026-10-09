package com.lacivita.turnos.notifications.application;

import com.lacivita.turnos.booking.AppointmentDetails;
import com.lacivita.turnos.booking.AppointmentDirectory;
import com.lacivita.turnos.notifications.domain.Audience;
import com.lacivita.turnos.notifications.domain.CalendarInvite;
import com.lacivita.turnos.notifications.domain.EmailContent;
import com.lacivita.turnos.notifications.domain.EmailContent.Action;
import com.lacivita.turnos.notifications.domain.EmailLayout;
import com.lacivita.turnos.notifications.domain.MessageTemplateRepository;
import com.lacivita.turnos.notifications.domain.Notification;
import com.lacivita.turnos.notifications.domain.NotificationType;
import com.lacivita.turnos.notifications.domain.OutgoingMessage;
import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.users.AccountDirectory;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Arma el mensaje de una notificación en el momento de enviarla, con los datos actuales del turno: si el
 * turno cambió mientras el aviso esperaba, el email muestra lo vigente. Si el aviso ya no corresponde (por
 * ejemplo, la confirmación de un turno que se canceló), lo descarta.
 */
@Component
class MessageComposer {

    /** Un resumen de agenda que no salió a tiempo deja de servir. */
    static final Duration AGENDA_VALIDITY = Duration.ofHours(12);

    private static final EnumSet<NotificationType> WITH_CALENDAR = EnumSet.of(
            NotificationType.APPOINTMENT_BOOKED,
            NotificationType.APPOINTMENT_RESCHEDULED,
            NotificationType.APPOINTMENT_CANCELLED);

    private final AppointmentDirectory appointments;
    private final AccountDirectory accounts;
    private final MessageTemplateRepository templates;
    private final EmailLayout layout;

    MessageComposer(
            AppointmentDirectory appointments,
            AccountDirectory accounts,
            MessageTemplateRepository templates,
            EmailLayout layout) {
        this.appointments = appointments;
        this.accounts = accounts;
        this.templates = templates;
        this.layout = layout;
    }

    /** Resultado de armar un mensaje: listo para enviar o descartado con un motivo. */
    sealed interface Composition {}

    record Ready(OutgoingMessage message) implements Composition {}

    record Skip(String reason) implements Composition {}

    Composition compose(Notification notification, Instant now) {
        if (notification.getType() == NotificationType.DAILY_AGENDA) {
            return dailyAgenda(notification, now);
        }
        var appointmentId = notification.appointmentId().orElseThrow();
        var found = appointments.details(notification.getBusinessId(), appointmentId);
        if (found.isEmpty()) {
            return new Skip("appointment_not_found");
        }
        var appointment = found.get();
        return notification.getAudience() == Audience.CUSTOMER
                ? toCustomer(notification, appointment, now)
                : toBarber(notification, appointment);
    }

    private Composition toCustomer(Notification notification, AppointmentDetails appointment, Instant now) {
        var type = notification.getType();
        if (!stillApplies(type, appointment, now)) {
            return new Skip("no_longer_applies");
        }
        Email to = appointment.customerEmail();
        if (to == null) {
            return new Skip("no_email");
        }
        var text = templates.textFor(type).fill(NotificationTexts.variables(appointment));
        var content = new EmailContent(
                text.subject(),
                paragraphs(text.body()),
                NotificationTexts.customerDetails(appointment),
                List.of(),
                appointment.isUpcoming() ? actions(appointment) : List.of(),
                NotificationTexts.customerFooter(appointment.businessName()));
        var attachments = WITH_CALENDAR.contains(type)
                ? List.of(CalendarInvite.of(appointment, now))
                : List.<OutgoingMessage.Attachment>of();
        return ready(to, content, attachments);
    }

    private Composition toBarber(Notification notification, AppointmentDetails appointment) {
        var barberId = notification.recipientUserId().orElseThrow();
        var to = accounts.emailOf(barberId);
        if (to.isEmpty()) {
            return new Skip("no_email");
        }
        String subject;
        String message;
        switch (notification.getType()) {
            case APPOINTMENT_BOOKED -> {
                if (!appointment.isUpcoming()) {
                    return new Skip("no_longer_applies");
                }
                subject = NotificationTexts.bookedSubject(appointment);
                message = NotificationTexts.bookedMessage(appointment);
            }
            case APPOINTMENT_RESCHEDULED -> {
                boolean stillTheirs = appointment.barberId().equals(barberId);
                subject = stillTheirs
                        ? NotificationTexts.movedSubject(appointment)
                        : NotificationTexts.reassignedSubject();
                message = stillTheirs
                        ? NotificationTexts.movedMessage(appointment)
                        : NotificationTexts.reassignedMessage(appointment);
            }
            case APPOINTMENT_CANCELLED -> {
                if (!appointment.isCancelled()) {
                    return new Skip("no_longer_applies");
                }
                subject = NotificationTexts.cancelledSubject(appointment);
                message = NotificationTexts.cancelledMessage(appointment);
            }
            default -> throw new IllegalStateException("Aviso sin email al equipo: " + notification.getType());
        }
        var content = new EmailContent(
                subject,
                List.of(message),
                NotificationTexts.teamDetails(appointment),
                List.of(),
                List.of(),
                NotificationTexts.teamFooter(appointment.businessName()));
        return ready(to.get(), content, List.of());
    }

    private Composition dailyAgenda(Notification notification, Instant now) {
        if (now.isAfter(notification.getDueAt().plus(AGENDA_VALIDITY))) {
            return new Skip("too_late");
        }
        var barberId = notification.recipientUserId().orElseThrow();
        var to = accounts.emailOf(barberId);
        if (to.isEmpty()) {
            return new Skip("no_email");
        }
        LocalDate day = notification.agendaDate().orElseThrow();
        // Se busca con margen y se filtra por el día en la zona de cada sucursal.
        var searchFrom = day.minusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        var searchTo = day.plusDays(2).atStartOfDay().toInstant(ZoneOffset.UTC);
        var ofTheDay =
                appointments.upcomingForBarber(notification.getBusinessId(), barberId, searchFrom, searchTo).stream()
                        .filter(appointment -> LocalDate.ofInstant(appointment.startsAt(), appointment.timeZone())
                                .equals(day))
                        .toList();
        if (ofTheDay.isEmpty()) {
            return new Skip("empty_agenda");
        }
        String businessName = ofTheDay.getFirst().businessName();
        var content = new EmailContent(
                NotificationTexts.agendaSubject(businessName),
                List.of(NotificationTexts.agendaMessage(ofTheDay.size())),
                List.of(),
                ofTheDay.stream().map(NotificationTexts::agendaItem).toList(),
                List.of(),
                NotificationTexts.teamFooter(businessName));
        return ready(to.get(), content, List.of());
    }

    private static boolean stillApplies(NotificationType type, AppointmentDetails appointment, Instant now) {
        return switch (type) {
            case APPOINTMENT_BOOKED, APPOINTMENT_RESCHEDULED -> appointment.isUpcoming();
            case APPOINTMENT_REMINDER -> appointment.isUpcoming() && now.isBefore(appointment.startsAt());
            case APPOINTMENT_CANCELLED -> appointment.isCancelled();
            case DAILY_AGENDA -> throw new IllegalArgumentException("El resumen de agenda es para el equipo");
        };
    }

    /** Botones con un link nuevo del cliente: cada email lleva el suyo. */
    private List<Action> actions(AppointmentDetails appointment) {
        String link = appointments.newManageLink(appointment.businessId(), appointment.id());
        var actions = new ArrayList<Action>();
        if (appointment.isPending()) {
            actions.add(new Action(NotificationTexts.CONFIRM, link + "&accion=confirmar"));
        }
        actions.add(new Action(NotificationTexts.RESCHEDULE, link + "&accion=reprogramar"));
        actions.add(new Action(NotificationTexts.CANCEL, link + "&accion=cancelar"));
        return actions;
    }

    /** Los párrafos van separados por una línea en blanco; los saltos simples se respetan dentro del párrafo. */
    private static List<String> paragraphs(String body) {
        return Arrays.stream(body.split("\\n\\s*\\n"))
                .map(String::strip)
                .filter(paragraph -> !paragraph.isEmpty())
                .toList();
    }

    private Ready ready(Email to, EmailContent content, List<OutgoingMessage.Attachment> attachments) {
        return new Ready(new OutgoingMessage(
                to.value(), content.subject(), content.plainText(), layout.html(content), attachments));
    }
}
