package com.lacivita.turnos.users.domain;

import java.util.Locale;
import java.util.Optional;

/** Proveedores externos con los que se puede iniciar sesión. */
public enum IdentityProvider {
    GOOGLE;

    public static Optional<IdentityProvider> fromName(String name) {
        for (IdentityProvider provider : values()) {
            if (provider.name().equals(name.toUpperCase(Locale.ROOT))) {
                return Optional.of(provider);
            }
        }
        return Optional.empty();
    }
}
