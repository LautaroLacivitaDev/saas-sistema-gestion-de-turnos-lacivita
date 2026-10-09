package com.lacivita.turnos.booking.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuración de Cloudflare Turnstile (prefijo {@code app.turnstile}).
 *
 * @param secretKey clave secreta del sitio. Vacía desactiva la verificación (desarrollo y pruebas); en
 *     producción es obligatoria
 */
@ConfigurationProperties(prefix = "app.turnstile")
public record TurnstileProperties(String secretKey) {

    boolean isEnabled() {
        return secretKey != null && !secretKey.isBlank();
    }
}
