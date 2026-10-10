package com.lacivita.turnos.notifications.infrastructure;

import com.lacivita.turnos.notifications.domain.DemoEmail;
import com.lacivita.turnos.shared.mail.MailMessage;
import com.lacivita.turnos.shared.mail.Mailer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

/**
 * Emails de la cuenta y código de la reserva en el modo demo: van al buzón de la demo en lugar de salir
 * por SMTP. Reemplaza al envío real (que solo existe fuera del modo demo).
 */
@Component
@ConditionalOnBooleanProperty("app.demo.enabled")
class DemoMailer implements Mailer {

    private final DemoEmailStore store;

    DemoMailer(DemoEmailStore store) {
        this.store = store;
    }

    @Override
    public void sendAfterCommit(MailMessage message) {
        store.store(DemoEmail.text(
                message.to(), message.subject(), message.body(), store.clock().instant()));
    }
}
