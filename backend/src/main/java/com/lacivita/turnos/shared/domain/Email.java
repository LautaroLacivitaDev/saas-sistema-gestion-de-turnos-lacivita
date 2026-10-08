package com.lacivita.turnos.shared.domain;

import java.io.Serializable;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Dirección de email válida y normalizada (sin espacios y en minúsculas), para que la misma persona
 * no pueda registrarse dos veces cambiando mayúsculas.
 */
public record Email(String value) implements Serializable {

    private static final int MAX_LENGTH = 254;

    // Validación deliberadamente simple: la prueba real de que el email existe es el link de verificación.
    private static final Pattern FORMAT = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public Email {
        if (value == null || value.isBlank()) {
            throw new InvalidValueException("invalid_email", "El email es obligatorio.");
        }
        value = value.strip().toLowerCase(Locale.ROOT);
        if (value.length() > MAX_LENGTH || !FORMAT.matcher(value).matches()) {
            throw new InvalidValueException("invalid_email", "El email no tiene un formato válido.");
        }
    }

    public static Email of(String value) {
        return new Email(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
