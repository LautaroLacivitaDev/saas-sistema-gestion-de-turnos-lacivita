package com.lacivita.turnos.catalog.domain;

import com.lacivita.turnos.shared.domain.InvalidValueException;
import com.lacivita.turnos.shared.domain.Money;
import jakarta.persistence.Embeddable;

/**
 * Rango en el que cada barbero elige su precio para un servicio. Lo fija el dueño; un precio fuera del
 * rango necesita la aprobación de un gerente.
 */
@Embeddable
public record PriceRange(Money min, Money max) {

    public PriceRange {
        if (min == null || max == null) {
            throw new InvalidValueException("invalid_price_range", "El rango necesita un mínimo y un máximo.");
        }
        if (min.isGreaterThan(max)) {
            throw new InvalidValueException("invalid_price_range", "El mínimo no puede ser mayor que el máximo.");
        }
    }

    public boolean contains(Money price) {
        return !price.isLessThan(min) && !price.isGreaterThan(max);
    }
}
