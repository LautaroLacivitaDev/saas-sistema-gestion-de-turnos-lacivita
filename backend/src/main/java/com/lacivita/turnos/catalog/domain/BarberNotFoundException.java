package com.lacivita.turnos.catalog.domain;

import com.lacivita.turnos.shared.domain.NotFoundException;

/** La persona no forma parte del equipo del negocio. */
public class BarberNotFoundException extends NotFoundException {

    public BarberNotFoundException() {
        super("barber_not_found", "Esa persona no forma parte del equipo.");
    }
}
