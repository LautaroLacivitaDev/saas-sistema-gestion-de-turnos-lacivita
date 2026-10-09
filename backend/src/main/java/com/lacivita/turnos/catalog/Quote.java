package com.lacivita.turnos.catalog;

import com.lacivita.turnos.shared.domain.Money;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Precio y duración con los que un profesional hace un servicio o combo hoy, con el detalle de cada
 * servicio (un combo tiene varios). Las reservas copian este detalle en el turno.
 */
public record Quote(Money price, Duration duration, List<Line> lines) {

    public Quote {
        Objects.requireNonNull(price, "price");
        Objects.requireNonNull(duration, "duration");
        lines = List.copyOf(lines);
    }

    /** Un servicio de lo que se reserva, en el orden en que se hace. */
    public record Line(UUID serviceId, String serviceName, Money price, Duration duration) {}
}
