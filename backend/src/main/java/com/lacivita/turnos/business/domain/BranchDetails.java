package com.lacivita.turnos.business.domain;

import com.lacivita.turnos.shared.domain.InvalidValueException;
import com.lacivita.turnos.shared.domain.PhoneNumber;
import java.time.ZoneId;
import java.util.Objects;

/**
 * Datos editables de una sucursal.
 *
 * @param coordinates ubicación en el mapa; nula si no se informa
 * @param phone teléfono de la sucursal; nulo si no se informa
 * @param timeZone zona horaria con la que se muestran y calculan los turnos de la sucursal
 */
public record BranchDetails(String name, Address address, Coordinates coordinates, PhoneNumber phone, ZoneId timeZone) {

    static final int MAX_NAME_LENGTH = 80;

    public BranchDetails {
        if (name == null || name.isBlank()) {
            throw new InvalidValueException("invalid_branch_name", "El nombre de la sucursal es obligatorio.");
        }
        name = name.strip();
        if (name.length() > MAX_NAME_LENGTH) {
            throw new InvalidValueException(
                    "invalid_branch_name", "El nombre puede tener hasta " + MAX_NAME_LENGTH + " caracteres.");
        }
        Objects.requireNonNull(address, "address");
        Objects.requireNonNull(timeZone, "timeZone");
    }
}
