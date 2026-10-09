package com.lacivita.turnos.booking.domain;

import com.lacivita.turnos.shared.domain.RuleViolationException;

/** El horario estuvo reservado unos minutos y venció antes de confirmarlo. */
public class HoldExpiredException extends RuleViolationException {

    public HoldExpiredException() {
        super("hold_expired", "Pasó el tiempo para confirmar ese horario. Elegí uno de nuevo.");
    }
}
