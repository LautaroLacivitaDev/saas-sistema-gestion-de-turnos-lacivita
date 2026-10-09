package com.lacivita.turnos.booking.domain;

import com.lacivita.turnos.shared.domain.RuleViolationException;

/** El profesional no hace lo que se quiere reservar (o el servicio está retirado). */
public class NotOfferedException extends RuleViolationException {

    public NotOfferedException() {
        super("not_offered", "Ese profesional no hace ese servicio.");
    }
}
