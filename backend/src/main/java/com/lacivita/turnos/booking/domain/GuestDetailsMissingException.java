package com.lacivita.turnos.booking.domain;

import com.lacivita.turnos.shared.domain.RuleViolationException;

/** Se quiere confirmar como invitado sin haber pedido el código. */
public class GuestDetailsMissingException extends RuleViolationException {

    public GuestDetailsMissingException() {
        super("guest_details_missing", "Primero pedí el código con tus datos.");
    }
}
