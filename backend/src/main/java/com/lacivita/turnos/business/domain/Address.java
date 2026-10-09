package com.lacivita.turnos.business.domain;

import com.lacivita.turnos.shared.domain.InvalidValueException;
import jakarta.persistence.Embeddable;

/**
 * Dirección de una sucursal. La ciudad y el barrio sirven también como filtros del buscador.
 *
 * @param neighborhood barrio; nulo si no se informa
 */
@Embeddable
public record Address(String street, String neighborhood, String city) {

    public Address {
        street = required(street, 150, "invalid_street", "La dirección");
        city = required(city, 80, "invalid_city", "La ciudad");
        neighborhood = neighborhood == null || neighborhood.isBlank() ? null : neighborhood.strip();
        if (neighborhood != null && neighborhood.length() > 80) {
            throw new InvalidValueException("invalid_neighborhood", "El barrio puede tener hasta 80 caracteres.");
        }
    }

    /** La dirección en una línea, para mostrar: "calle, barrio, ciudad" (sin barrio si no se informó). */
    public String oneLine() {
        return neighborhood == null ? street + ", " + city : street + ", " + neighborhood + ", " + city;
    }

    private static String required(String value, int maxLength, String code, String label) {
        if (value == null || value.isBlank()) {
            throw new InvalidValueException(code, label + " es obligatoria.");
        }
        String trimmed = value.strip();
        if (trimmed.length() > maxLength) {
            throw new InvalidValueException(code, label + " puede tener hasta " + maxLength + " caracteres.");
        }
        return trimmed;
    }
}
