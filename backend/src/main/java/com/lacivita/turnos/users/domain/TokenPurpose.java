package com.lacivita.turnos.users.domain;

import java.time.Duration;

/** Para qué sirve un token de un solo uso, y cuánto dura. */
public enum TokenPurpose {
    EMAIL_VERIFICATION(Duration.ofHours(24)),
    LOGIN_LINK(Duration.ofMinutes(15));

    private final Duration validity;

    TokenPurpose(Duration validity) {
        this.validity = validity;
    }

    public Duration validity() {
        return validity;
    }
}
