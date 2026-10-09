package com.lacivita.turnos.schedule.domain;

import com.lacivita.turnos.shared.domain.NotFoundException;

/** El link no corresponde a ningún negocio. */
public class UnknownBusinessException extends NotFoundException {

    public UnknownBusinessException() {
        super("business_not_found", "No encontramos ese negocio.");
    }
}
