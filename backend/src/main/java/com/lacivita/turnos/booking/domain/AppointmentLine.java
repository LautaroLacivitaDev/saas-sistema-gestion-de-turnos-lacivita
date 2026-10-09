package com.lacivita.turnos.booking.domain;

import com.lacivita.turnos.shared.domain.Money;
import jakarta.persistence.Embeddable;
import java.time.Duration;
import java.util.Objects;
import java.util.UUID;

/**
 * Un servicio del turno con el precio y la duración vigentes al reservar. Es una copia: si después el
 * catálogo cambia, el turno y los reportes no cambian.
 */
@Embeddable
public record AppointmentLine(UUID serviceId, String serviceName, Money price, int durationMinutes) {

    public AppointmentLine {
        Objects.requireNonNull(serviceId, "serviceId");
        Objects.requireNonNull(serviceName, "serviceName");
        Objects.requireNonNull(price, "price");
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("La duración tiene que ser positiva");
        }
    }

    public Duration duration() {
        return Duration.ofMinutes(durationMinutes);
    }
}
