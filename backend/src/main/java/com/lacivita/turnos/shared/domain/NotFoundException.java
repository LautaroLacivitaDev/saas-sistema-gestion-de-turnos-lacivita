package com.lacivita.turnos.shared.domain;

/** Lo que se busca no existe (o no es visible para quien lo pide). Se responde con 404. */
public abstract non-sealed class NotFoundException extends DomainException {

    protected NotFoundException(String code, String message) {
        super(code, message);
    }
}
