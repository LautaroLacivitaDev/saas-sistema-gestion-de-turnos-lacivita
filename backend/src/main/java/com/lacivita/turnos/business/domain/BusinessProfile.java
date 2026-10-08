package com.lacivita.turnos.business.domain;

import com.lacivita.turnos.shared.domain.InvalidValueException;
import java.util.Objects;

/**
 * Datos editables del negocio que se muestran en su página pública.
 *
 * @param description texto libre; nulo si no hay
 * @param searchable si aparece en el buscador; si no, solo se llega por el link directo
 */
public record BusinessProfile(String name, BusinessCategory category, String description, boolean searchable) {

    static final int MAX_NAME_LENGTH = 80;
    static final int MAX_DESCRIPTION_LENGTH = 1000;

    public BusinessProfile {
        if (name == null || name.isBlank()) {
            throw new InvalidValueException("invalid_business_name", "El nombre del negocio es obligatorio.");
        }
        name = name.strip();
        if (name.length() > MAX_NAME_LENGTH) {
            throw new InvalidValueException(
                    "invalid_business_name", "El nombre puede tener hasta " + MAX_NAME_LENGTH + " caracteres.");
        }
        Objects.requireNonNull(category, "category");
        description = description == null || description.isBlank() ? null : description.strip();
        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new InvalidValueException(
                    "invalid_description",
                    "La descripción puede tener hasta " + MAX_DESCRIPTION_LENGTH + " caracteres.");
        }
    }
}
