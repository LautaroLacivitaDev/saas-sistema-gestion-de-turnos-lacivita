package com.lacivita.turnos.shared.domain;

/** La operación choca con el estado actual de otro recurso (por ejemplo, un dato único repetido). Se responde con 409. */
public abstract non-sealed class ConflictException extends DomainException {

    protected ConflictException(String code, String message) {
        super(code, message);
    }
}
