package com.lacivita.turnos.booking.application;

import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.shared.mail.MailMessage;
import com.lacivita.turnos.shared.mail.Mailer;
import org.springframework.stereotype.Component;

/** Código de verificación para quien reserva como invitado. */
// DECISIÓN: el código no pasa por la bandeja de salida de notificaciones. Lo espera la persona mientras
// reserva y vence en 10 minutos: un reintento posterior no sirve, y si no llega pide otro. Tampoco se
// guarda el código en claro en ninguna tabla.
@Component
class BookingEmails {

    private final Mailer mailer;

    BookingEmails(Mailer mailer) {
        this.mailer = mailer;
    }

    void sendCode(Email to, String name, String businessName, String code) {
        mailer.sendAfterCommit(
                new MailMessage(to, "Tu código para reservar en " + businessName, """
                Hola %s:

                Tu código para confirmar el turno es: %s

                Vence en 10 minutos. Si no estás reservando un turno, ignorá este mensaje.
                """.formatted(name, code)));
    }
}
