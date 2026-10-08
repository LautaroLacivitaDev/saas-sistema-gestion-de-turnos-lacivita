package com.lacivita.turnos.shared.domain;

/**
 * Base de las excepciones de negocio. Cada módulo define subclases con nombre de negocio
 * ({@code EmailAlreadyRegisteredException}, no {@code IllegalStateException}).
 *
 * <p>El manejador global de errores traduce cada familia a un estado HTTP: {@link NotFoundException}
 * a 404, {@link ConflictException} a 409, {@link RuleViolationException} a 422 e {@link
 * InvalidValueException} a 400. El mensaje se muestra al usuario, así que va en español.
 */
public abstract sealed class DomainException extends RuntimeException
        permits NotFoundException, ConflictException, RuleViolationException, InvalidValueException {

    private final String code;

    protected DomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    /** Código estable y legible por máquina, para que el frontend reaccione sin parsear el mensaje. */
    public String code() {
        return code;
    }
}
