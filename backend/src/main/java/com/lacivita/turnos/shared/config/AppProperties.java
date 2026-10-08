package com.lacivita.turnos.shared.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.net.URI;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuración propia de la aplicación (prefijo {@code app}). Se valida al arrancar: si falta un valor,
 * la aplicación no inicia en lugar de fallar más tarde.
 *
 * @param frontendUrl dirección pública del frontend, para armar los links de los emails y las
 *     redirecciones del login con Google
 * @param mail remitente de los emails
 * @param rateLimit límite de intentos en los endpoints públicos que lo piden
 */
@Validated
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        @NotNull URI frontendUrl,
        @NotNull @Valid Mail mail,
        @NotNull @Valid RateLimit rateLimit) {

    /** Arma un link absoluto del frontend, por ejemplo {@code link("/acceso?token=...")}. */
    public String frontendLink(String pathAndQuery) {
        String base = frontendUrl.toString().replaceAll("/+$", "");
        return base + pathAndQuery;
    }

    public record Mail(
            @NotBlank @Email String from, @NotBlank String fromName) {}

    /** @param requestsPerMinute intentos por minuto y por IP en cada endpoint público limitado */
    public record RateLimit(@Positive int requestsPerMinute) {}
}
