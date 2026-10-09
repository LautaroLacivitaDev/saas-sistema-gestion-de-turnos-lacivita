package com.lacivita.turnos.booking.domain;

import com.lacivita.turnos.shared.domain.RuleViolationException;

public class BarberNotInBranchException extends RuleViolationException {

    public BarberNotInBranchException() {
        super("barber_not_in_branch", "Esa persona no trabaja en esa sucursal.");
    }
}
