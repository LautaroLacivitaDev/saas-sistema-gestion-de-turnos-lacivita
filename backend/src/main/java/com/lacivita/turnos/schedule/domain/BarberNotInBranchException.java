package com.lacivita.turnos.schedule.domain;

import com.lacivita.turnos.shared.domain.RuleViolationException;

/** El horario es de una sucursal en la que la persona no trabaja. */
public class BarberNotInBranchException extends RuleViolationException {

    public BarberNotInBranchException() {
        super("barber_not_in_branch", "Esa persona no trabaja en esa sucursal. Asignale la sucursal desde el equipo.");
    }
}
