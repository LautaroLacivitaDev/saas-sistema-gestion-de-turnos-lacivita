package com.lacivita.turnos.shared.domain;

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * Número de teléfono normalizado: solo dígitos, con {@code +} inicial opcional. Acepta lo que escribe
 * una persona ("11 4567-8901", "+54 9 11 4567 8901") y quita espacios, guiones y paréntesis.
 */
public record PhoneNumber(String value) implements Serializable {

    private static final Pattern NORMALIZED = Pattern.compile("^\\+?\\d{6,15}$");
    private static final Pattern SEPARATORS = Pattern.compile("[\\s().-]");

    public PhoneNumber {
        if (value == null || value.isBlank()) {
            throw new InvalidValueException("invalid_phone", "El teléfono es obligatorio.");
        }
        value = SEPARATORS.matcher(value.strip()).replaceAll("");
        if (!NORMALIZED.matcher(value).matches()) {
            throw new InvalidValueException("invalid_phone", "El teléfono no tiene un formato válido.");
        }
    }

    @Override
    public String toString() {
        return value;
    }
}
