package com.lacivita.turnos.shared.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Rol de una persona en la plataforma, independiente de cualquier negocio.
 *
 * <p>Los roles dentro de un negocio (dueño, gerente, barbero) son {@link BusinessRole} y dependen de la
 * membresía. Cualquier usuario autenticado es cliente: no hace falta un rol para eso.
 */
public enum PlatformRole {
    USER,
    /** Dueño de la aplicación o soporte técnico. Hereda todo lo que puede hacer {@link #USER}. */
    ADMIN;

    SimpleGrantedAuthority authority() {
        return new SimpleGrantedAuthority("ROLE_" + name());
    }
}
