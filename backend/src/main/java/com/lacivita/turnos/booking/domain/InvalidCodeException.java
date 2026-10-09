package com.lacivita.turnos.booking.domain;

import com.lacivita.turnos.shared.domain.RuleViolationException;

public class InvalidCodeException extends RuleViolationException {

    public InvalidCodeException() {
        super("invalid_code", "El código no es correcto o venció. Revisalo o pedí uno nuevo.");
    }
}
