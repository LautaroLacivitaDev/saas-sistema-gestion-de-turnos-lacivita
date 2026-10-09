package com.lacivita.turnos.shared.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Importe en pesos con dos decimales. Nunca negativo.
 *
 * <p>Se compara por valor: {@code 1500} y {@code 1500.00} son el mismo importe.
 */
// DECISIÓN: sin moneda explícita. El MVP opera solo en pesos argentinos; si se suman otras monedas,
// se agrega el campo y una migración que complete ARS en los datos existentes.
public record Money(BigDecimal amount) implements Comparable<Money> {

    // Antes que ZERO: el constructor los usa al inicializar la clase.
    private static final int SCALE = 2;
    private static final BigDecimal MAX = new BigDecimal("99999999.99");

    public static final Money ZERO = new Money(BigDecimal.ZERO);

    public Money {
        if (amount == null) {
            throw new InvalidValueException("invalid_amount", "El importe es obligatorio.");
        }
        if (amount.signum() < 0) {
            throw new InvalidValueException("invalid_amount", "El importe no puede ser negativo.");
        }
        if (amount.stripTrailingZeros().scale() > SCALE) {
            throw new InvalidValueException("invalid_amount", "El importe admite hasta dos decimales.");
        }
        if (amount.compareTo(MAX) > 0) {
            throw new InvalidValueException("invalid_amount", "El importe es demasiado alto.");
        }
        amount = amount.setScale(SCALE, RoundingMode.UNNECESSARY);
    }

    public static Money of(String amount) {
        return new Money(new BigDecimal(amount));
    }

    public Money plus(Money other) {
        return new Money(amount.add(other.amount));
    }

    public boolean isLessThan(Money other) {
        return compareTo(other) < 0;
    }

    public boolean isGreaterThan(Money other) {
        return compareTo(other) > 0;
    }

    @Override
    public int compareTo(Money other) {
        return amount.compareTo(Objects.requireNonNull(other, "other").amount);
    }

    @Override
    public String toString() {
        return amount.toPlainString();
    }
}
