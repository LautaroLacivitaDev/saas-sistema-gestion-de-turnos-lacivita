package com.lacivita.turnos.booking.domain;

import com.lacivita.turnos.shared.domain.NotFoundException;

public class BarberNotFoundException extends NotFoundException {

    public BarberNotFoundException() {
        super("barber_not_found", "Esa persona no forma parte del equipo.");
    }
}
