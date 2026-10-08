package com.lacivita.turnos.users.domain;

import com.lacivita.turnos.shared.domain.ConflictException;

public class IdentityAlreadyLinkedException extends ConflictException {

    IdentityAlreadyLinkedException(IdentityProvider provider) {
        super("identity_already_linked", "Tu cuenta ya está vinculada a otra cuenta de " + displayName(provider) + ".");
    }

    private static String displayName(IdentityProvider provider) {
        return switch (provider) {
            case GOOGLE -> "Google";
        };
    }
}
