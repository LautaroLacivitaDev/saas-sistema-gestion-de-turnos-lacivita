package com.lacivita.turnos.shared.mail;

import com.lacivita.turnos.shared.domain.Email;
import java.util.Objects;

/** Email de texto listo para enviar. */
public record MailMessage(Email to, String subject, String body) {

    public MailMessage {
        Objects.requireNonNull(to, "to");
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("El asunto es obligatorio");
        }
        if (body == null || body.isBlank()) {
            throw new IllegalArgumentException("El cuerpo es obligatorio");
        }
    }
}
