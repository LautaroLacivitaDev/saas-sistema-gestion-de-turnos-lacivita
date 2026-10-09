package com.lacivita.turnos.notifications.domain;

/** Diseño HTML de los emails, pensado primero para el celular. */
public interface EmailLayout {

    /** El email en HTML. Todo el texto del contenido sale escapado. */
    String html(EmailContent content);
}
