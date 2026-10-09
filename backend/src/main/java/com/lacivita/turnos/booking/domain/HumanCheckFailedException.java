package com.lacivita.turnos.booking.domain;

import com.lacivita.turnos.shared.domain.RuleViolationException;

public class HumanCheckFailedException extends RuleViolationException {

    public HumanCheckFailedException() {
        super("human_check_failed", "No pudimos verificar que no seas un robot. Probá de nuevo.");
    }
}
