package com.lacivita.turnos.business.domain;

import com.lacivita.turnos.shared.domain.InvalidValueException;
import jakarta.persistence.Embeddable;
import java.math.BigDecimal;
import java.math.RoundingMode;

/** Ubicación de una sucursal en el mapa, con 6 decimales (unos 10 cm de precisión). */
@Embeddable
public record Coordinates(BigDecimal latitude, BigDecimal longitude) {

    private static final BigDecimal MAX_LATITUDE = BigDecimal.valueOf(90);
    private static final BigDecimal MAX_LONGITUDE = BigDecimal.valueOf(180);

    public Coordinates {
        if (latitude == null || longitude == null) {
            throw new InvalidValueException("invalid_coordinates", "Faltan la latitud o la longitud.");
        }
        if (latitude.abs().compareTo(MAX_LATITUDE) > 0 || longitude.abs().compareTo(MAX_LONGITUDE) > 0) {
            throw new InvalidValueException("invalid_coordinates", "Las coordenadas están fuera de rango.");
        }
        latitude = latitude.setScale(6, RoundingMode.HALF_UP);
        longitude = longitude.setScale(6, RoundingMode.HALF_UP);
    }
}
