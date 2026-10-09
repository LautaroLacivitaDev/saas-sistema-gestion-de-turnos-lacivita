package com.lacivita.turnos.catalog.domain;

import com.lacivita.turnos.shared.domain.Money;
import java.util.Objects;

/** Precio y duración con los que un barbero hace un servicio o un combo. */
public record Terms(Money price, ServiceDuration duration) {

    public Terms {
        Objects.requireNonNull(price, "price");
        Objects.requireNonNull(duration, "duration");
    }

    public Terms plus(Terms other) {
        return new Terms(price.plus(other.price), duration.plus(other.duration));
    }
}
