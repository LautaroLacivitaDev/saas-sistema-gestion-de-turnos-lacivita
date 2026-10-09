package com.lacivita.turnos.booking.application;

import com.lacivita.turnos.booking.domain.Appointment;
import com.lacivita.turnos.booking.domain.AppointmentLine;
import com.lacivita.turnos.booking.domain.ManageToken;
import com.lacivita.turnos.business.BranchSummary;
import com.lacivita.turnos.shared.config.AppProperties;
import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.shared.mail.MailMessage;
import com.lacivita.turnos.shared.mail.Mailer;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Emails de la reserva online: el código para quien reserva como invitado y la confirmación con el link
 * para gestionar el turno.
 */
// DECISIÓN: texto plano y sin reintentos, como los emails de la cuenta. Los avisos de turnos (con
// plantillas, .ics, recordatorios y reintentos) llegan con el módulo de notificaciones en el Hito 7.
@Component
class BookingEmails {

    static final String MANAGE_PATH = "/turno?token=";
    private static final DateTimeFormatter WHEN =
            DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'a las' HH:mm", Locale.of("es", "AR"));

    private final Mailer mailer;
    private final AppProperties properties;

    BookingEmails(Mailer mailer, AppProperties properties) {
        this.mailer = mailer;
        this.properties = properties;
    }

    void sendCode(Email to, String name, String businessName, String code) {
        mailer.sendAfterCommit(
                new MailMessage(to, "Tu código para reservar en " + businessName, """
                Hola %s:

                Tu código para confirmar el turno es: %s

                Vence en 10 minutos. Si no estás reservando un turno, ignorá este mensaje.
                """.formatted(name, code)));
    }

    void sendConfirmation(
            Email to,
            String name,
            String businessName,
            BranchSummary branch,
            String barberName,
            Appointment appointment,
            ManageToken token) {
        String link = properties.frontendLink(MANAGE_PATH + URLEncoder.encode(token.value(), StandardCharsets.UTF_8));
        String when = WHEN.format(appointment.getStartsAt().atZone(branch.timeZone()));
        String services = appointment.getLines().stream()
                .map(AppointmentLine::serviceName)
                .collect(Collectors.joining(" + "));
        mailer.sendAfterCommit(new MailMessage(to, "Tu turno en " + businessName + " está confirmado", """
                Hola %s:

                Tu turno quedó confirmado.

                %s con %s
                %s, sucursal %s
                Total: $%s

                Para ver, cancelar o cambiar el turno, entrá a este link:
                %s
                """.formatted(
                        name, services, barberName, when, branch.name(), appointment.getTotalPrice(), link)));
    }
}
