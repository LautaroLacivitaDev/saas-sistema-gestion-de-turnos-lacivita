package com.lacivita.turnos.booking.domain;

import com.lacivita.turnos.shared.domain.NotFoundException;

public class UnknownBusinessException extends NotFoundException {

    public UnknownBusinessException() {
        super("business_not_found", "No encontramos ese negocio.");
    }
}
