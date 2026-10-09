package com.lacivita.turnos.catalog.domain;

import com.lacivita.turnos.shared.domain.NotFoundException;

/** El barbero no ofrece ese servicio. */
public class OfferingNotFoundException extends NotFoundException {

    public OfferingNotFoundException() {
        super("offering_not_found", "Ese profesional no ofrece ese servicio.");
    }
}
