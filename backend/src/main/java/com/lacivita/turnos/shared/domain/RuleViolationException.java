package com.lacivita.turnos.shared.domain;

/** La operación es válida en forma pero viola una regla de negocio. Se responde con 422. */
public abstract non-sealed class RuleViolationException extends DomainException {

    protected RuleViolationException(String code, String message) {
        super(code, message);
    }
}
