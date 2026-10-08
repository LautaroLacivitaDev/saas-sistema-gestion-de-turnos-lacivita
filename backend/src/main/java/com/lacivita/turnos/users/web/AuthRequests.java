package com.lacivita.turnos.users.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Cuerpos de las solicitudes de autenticación. Validan forma; las reglas viven en el dominio. */
final class AuthRequests {

    private AuthRequests() {}

    record Register(
            @NotBlank(message = "Ingresá tu nombre.") @Size(max = 120, message = "El nombre es demasiado largo.")
            String name,

            @NotBlank(message = "Ingresá tu email.") @Email(message = "El email no tiene un formato válido.")
            String email,

            @NotBlank(message = "Ingresá una contraseña.")
            @Size(min = 8, max = 64, message = "La contraseña tiene que tener entre 8 y 64 caracteres.")
            String password) {

        @Override
        public String toString() {
            return "Register[name=%s, email=%s, password=***]".formatted(name, email);
        }
    }

    record Login(
            @NotBlank(message = "Ingresá tu email.") String email,
            @NotBlank(message = "Ingresá tu contraseña.") String password) {

        @Override
        public String toString() {
            return "Login[email=%s, password=***]".formatted(email);
        }
    }

    record LoginLink(
            @NotBlank(message = "Ingresá tu email.") @Email(message = "El email no tiene un formato válido.")
            String email) {}

    record Token(
            @NotBlank(message = "Falta el token.") @Size(max = 200)
            String token) {}
}
