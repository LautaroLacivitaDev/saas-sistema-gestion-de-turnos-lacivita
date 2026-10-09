package com.lacivita.turnos.notifications.domain;

import java.util.EnumSet;
import java.util.Set;

/** De qué avisa una notificación. */
public enum NotificationType {
    APPOINTMENT_BOOKED,
    APPOINTMENT_RESCHEDULED,
    APPOINTMENT_CANCELLED,
    APPOINTMENT_REMINDER,
    /** Resumen de los turnos del día para cada profesional. */
    DAILY_AGENDA;

    /** Los emails al cliente cuyo texto puede personalizar el negocio. */
    public static final Set<NotificationType> CUSTOMIZABLE =
            EnumSet.of(APPOINTMENT_BOOKED, APPOINTMENT_RESCHEDULED, APPOINTMENT_CANCELLED, APPOINTMENT_REMINDER);

    public boolean isCustomizable() {
        return CUSTOMIZABLE.contains(this);
    }
}
