package com.lacivita.turnos.shared.domain;

/**
 * Un objeto de valor recibió un dato inválido. Se responde con 400.
 *
 * <p>Es concreta porque la lanzan los constructores de los objetos de valor, cada uno con su propio
 * código ({@code invalid_email}, {@code invalid_slug}, etc.).
 */
public final class InvalidValueException extends DomainException {

    public InvalidValueException(String code, String message) {
        super(code, message);
    }
}
