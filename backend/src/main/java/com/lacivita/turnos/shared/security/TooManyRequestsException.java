package com.lacivita.turnos.shared.security;

import java.time.Duration;

/** Se superó el límite de intentos. Se responde con 429 y el encabezado {@code Retry-After}. */
public class TooManyRequestsException extends RuntimeException {

    private final Duration retryAfter;

    TooManyRequestsException(Duration retryAfter) {
        super("Demasiados intentos");
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
