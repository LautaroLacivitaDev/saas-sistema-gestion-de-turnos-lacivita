package com.lacivita.turnos.notifications.domain;

import com.lacivita.turnos.shared.domain.NotFoundException;

/** El turno no existe o quien consulta no lo puede ver. */
public class UnknownAppointmentException extends NotFoundException {

    public UnknownAppointmentException() {
        super("appointment_not_found", "No encontramos ese turno.");
    }
}
