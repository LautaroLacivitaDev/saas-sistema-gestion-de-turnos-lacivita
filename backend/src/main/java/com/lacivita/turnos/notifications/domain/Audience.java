package com.lacivita.turnos.notifications.domain;

/** A quién va dirigida una notificación. */
public enum Audience {
    /** El cliente del turno. Su email se toma del turno al enviar. */
    CUSTOMER,
    /** Un profesional del equipo. */
    BARBER
}
