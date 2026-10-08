package com.lacivita.turnos.business.domain;

import com.lacivita.turnos.shared.domain.NotFoundException;

public class BusinessNotFoundException extends NotFoundException {

    public BusinessNotFoundException() {
        super("business_not_found", "No encontramos ese negocio.");
    }
}
