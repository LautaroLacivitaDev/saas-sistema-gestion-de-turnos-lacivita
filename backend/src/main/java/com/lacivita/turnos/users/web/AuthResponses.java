package com.lacivita.turnos.users.web;

import com.lacivita.turnos.users.application.AccountView;
import java.util.UUID;

/**
 * Respuestas de la API de autenticación. Son el contrato HTTP: cambian solo de forma compatible, aunque
 * cambien los modelos internos de la aplicación.
 */
final class AuthResponses {

    private AuthResponses() {}

    record Account(
            UUID id, String name, String email, boolean emailVerified, boolean hasPassword, String platformRole) {

        static Account from(AccountView view) {
            return new Account(
                    view.id(),
                    view.name(),
                    view.email(),
                    view.emailVerified(),
                    view.hasPassword(),
                    view.platformRole());
        }
    }

    record Csrf(String headerName, String token) {}
}
