package com.lacivita.turnos.booking.domain;

import com.lacivita.turnos.shared.domain.InvalidValueException;

/**
 * Lo que el equipo anota de un cliente. Solo lo ve el equipo, nunca el cliente.
 *
 * @param notes notas libres, por ejemplo "Prefiere que lo llamen antes de confirmar"
 * @param preferences preferencias cortas, por ejemplo "Fade bajo, sin raya"
 */
public record CustomerNotes(String notes, String preferences) {

    static final int MAX_NOTES = 2000;
    static final int MAX_PREFERENCES = 500;

    public static final CustomerNotes NONE = new CustomerNotes(null, null);

    public CustomerNotes {
        notes = optional(notes, MAX_NOTES, "invalid_customer_notes", "Las notas");
        preferences = optional(preferences, MAX_PREFERENCES, "invalid_customer_preferences", "Las preferencias");
    }

    private static String optional(String value, int maxLength, String code, String label) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.strip();
        if (trimmed.length() > maxLength) {
            throw new InvalidValueException(code, label + " pueden tener hasta " + maxLength + " caracteres.");
        }
        return trimmed;
    }
}
