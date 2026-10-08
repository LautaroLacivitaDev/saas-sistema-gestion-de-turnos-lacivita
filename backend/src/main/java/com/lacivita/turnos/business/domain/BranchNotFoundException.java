package com.lacivita.turnos.business.domain;

import com.lacivita.turnos.shared.domain.NotFoundException;

public class BranchNotFoundException extends NotFoundException {

    public BranchNotFoundException() {
        super("branch_not_found", "No encontramos esa sucursal.");
    }
}
