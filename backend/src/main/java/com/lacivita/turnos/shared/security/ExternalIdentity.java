package com.lacivita.turnos.shared.security;

import java.util.Objects;

/**
 * Datos de una persona autenticada por un proveedor externo (por ejemplo Google).
 *
 * @param provider proveedor en mayúsculas, por ejemplo {@code GOOGLE}
 * @param subject identificador estable de la persona en ese proveedor
 * @param email email informado por el proveedor
 * @param emailVerified si el proveedor confirma que la persona controla ese email
 * @param name nombre para mostrar; puede faltar
 */
public record ExternalIdentity(String provider, String subject, String email, boolean emailVerified, String name) {

    public ExternalIdentity {
        Objects.requireNonNull(provider, "provider");
        Objects.requireNonNull(subject, "subject");
    }
}
