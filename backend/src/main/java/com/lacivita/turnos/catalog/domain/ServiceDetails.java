package com.lacivita.turnos.catalog.domain;

import com.lacivita.turnos.shared.domain.InvalidValueException;
import com.lacivita.turnos.shared.domain.Money;
import java.util.Objects;

/**
 * Datos editables de un servicio del catálogo.
 *
 * @param category agrupa los servicios en la página pública (por ejemplo "Cortes" o "Barba")
 * @param description opcional
 * @param baseDuration duración de referencia; la hereda cada barbero que no fije la propia
 * @param basePrice precio de referencia; lo hereda cada barbero que no fije el propio
 */
public record ServiceDetails(
        String name, String category, String description, ServiceDuration baseDuration, Money basePrice) {

    static final int MAX_NAME_LENGTH = 80;
    static final int MAX_CATEGORY_LENGTH = 40;
    static final int MAX_DESCRIPTION_LENGTH = 500;

    public ServiceDetails {
        name = required(name, MAX_NAME_LENGTH, "invalid_service_name", "El nombre", "Ingresá el nombre del servicio.");
        category = required(
                category, MAX_CATEGORY_LENGTH, "invalid_service_category", "La categoría", "Elegí una categoría.");
        description = description == null || description.isBlank() ? null : description.strip();
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new InvalidValueException(
                    "invalid_service_description",
                    "La descripción puede tener hasta " + MAX_DESCRIPTION_LENGTH + " caracteres.");
        }
        Objects.requireNonNull(baseDuration, "baseDuration").requireOneService();
        Objects.requireNonNull(basePrice, "basePrice");
    }

    private static String required(String value, int maxLength, String code, String label, String missing) {
        if (value == null || value.isBlank()) {
            throw new InvalidValueException(code, missing);
        }
        String stripped = value.strip();
        if (stripped.length() > maxLength) {
            throw new InvalidValueException(code, label + " puede tener hasta " + maxLength + " caracteres.");
        }
        return stripped;
    }
}
