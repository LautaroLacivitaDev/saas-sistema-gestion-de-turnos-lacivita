package com.lacivita.turnos.booking.domain;

/** Por dónde entró el turno. */
public enum AppointmentSource {
    /** Lo reservó el cliente en la página pública. */
    WEB,
    /** Lo cargó el equipo (un cliente que llamó o llegó sin reserva). */
    COUNTER
}
