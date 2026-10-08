package com.lacivita.turnos.shared.domain;

/**
 * La persona tiene acceso al recurso, pero una regla de negocio no le permite esta acción en particular
 * (por ejemplo, un gerente que intenta dar de baja a otro gerente). Se responde con 403.
 */
public abstract non-sealed class ForbiddenException extends DomainException {

    protected ForbiddenException(String code, String message) {
        super(code, message);
    }
}
