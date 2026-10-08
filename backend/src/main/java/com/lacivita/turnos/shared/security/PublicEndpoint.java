package com.lacivita.turnos.shared.security;

import java.util.Objects;
import org.springframework.http.HttpMethod;

/**
 * Endpoint de la API que no exige sesión. Lo declara el módulo dueño del endpoint (ver {@link
 * PublicEndpoints}), así la seguridad común no necesita conocer las rutas de cada módulo.
 *
 * @param rateLimited si se aplica el límite de intentos por IP (para endpoints que crean cuentas,
 *     prueban credenciales o envían emails)
 */
public record PublicEndpoint(HttpMethod method, String path, boolean rateLimited) {

    public PublicEndpoint {
        Objects.requireNonNull(method, "method");
        if (path == null || !path.startsWith("/api/")) {
            throw new IllegalArgumentException("Los endpoints públicos deben estar bajo /api/: " + path);
        }
    }

    public static PublicEndpoint open(HttpMethod method, String path) {
        return new PublicEndpoint(method, path, false);
    }

    public static PublicEndpoint rateLimited(HttpMethod method, String path) {
        return new PublicEndpoint(method, path, true);
    }
}
