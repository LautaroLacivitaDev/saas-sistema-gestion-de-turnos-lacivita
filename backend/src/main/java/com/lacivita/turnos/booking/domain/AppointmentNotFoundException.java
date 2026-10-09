package com.lacivita.turnos.booking.domain;

import com.lacivita.turnos.shared.domain.NotFoundException;

public class AppointmentNotFoundException extends NotFoundException {

    public AppointmentNotFoundException() {
        super("appointment_not_found", "No encontramos ese turno.");
    }
}
