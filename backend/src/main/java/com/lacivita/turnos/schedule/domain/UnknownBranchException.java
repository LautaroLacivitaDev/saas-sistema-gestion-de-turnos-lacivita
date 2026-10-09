package com.lacivita.turnos.schedule.domain;

import com.lacivita.turnos.shared.domain.NotFoundException;

public class UnknownBranchException extends NotFoundException {

    public UnknownBranchException() {
        super("branch_not_found", "No encontramos esa sucursal.");
    }
}
