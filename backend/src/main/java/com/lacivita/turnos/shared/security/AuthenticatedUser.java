package com.lacivita.turnos.shared.security;

import com.lacivita.turnos.shared.domain.Email;
import java.io.Serializable;
import java.security.Principal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;

/**
 * Persona con sesión iniciada. Es el principal de Spring Security en todos los métodos de login
 * (contraseña, link de acceso y Google), así el resto del código trabaja con un solo tipo.
 *
 * <p>Se guarda en la sesión (en la base de datos), por eso es serializable y contiene solo datos
 * estables. Los roles por negocio no se guardan acá: se consultan en cada verificación de permisos,
 * así un cambio de rol aplica en el momento.
 */
public record AuthenticatedUser(UUID id, Email email, String name, PlatformRole platformRole)
        implements Principal, Serializable {

    public AuthenticatedUser {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(platformRole, "platformRole");
    }

    /** Nombre del principal para Spring: el id, que no cambia aunque la persona cambie su email. */
    @Override
    public String getName() {
        return id.toString();
    }

    public boolean isAdmin() {
        return platformRole == PlatformRole.ADMIN;
    }

    List<GrantedAuthority> authorities() {
        return List.of(platformRole.authority());
    }
}
