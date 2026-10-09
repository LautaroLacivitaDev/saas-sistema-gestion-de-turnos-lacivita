package com.lacivita.turnos.notifications.domain;

import java.util.List;
import java.util.Objects;

/**
 * Mensaje listo para salir por un canal. Cada canal usa lo que le sirve: el email, el HTML y los adjuntos;
 * un canal de texto (WhatsApp, por ejemplo) solo el texto.
 *
 * @param to dirección del destinatario en el canal (para email, la casilla)
 */
public record OutgoingMessage(String to, String subject, String text, String html, List<Attachment> attachments) {

    public OutgoingMessage {
        Objects.requireNonNull(to, "to");
        Objects.requireNonNull(subject, "subject");
        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(html, "html");
        attachments = List.copyOf(attachments);
    }

    /** Archivo adjunto, por ejemplo el {@code .ics} para agregar el turno al calendario. */
    public record Attachment(String fileName, String contentType, byte[] content) {

        public Attachment {
            Objects.requireNonNull(fileName, "fileName");
            Objects.requireNonNull(contentType, "contentType");
            content = content.clone();
        }

        @Override
        public byte[] content() {
            return content.clone();
        }
    }
}
