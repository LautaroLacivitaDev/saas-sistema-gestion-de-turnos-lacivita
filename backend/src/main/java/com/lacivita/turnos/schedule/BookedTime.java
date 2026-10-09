package com.lacivita.turnos.schedule;

import com.lacivita.turnos.shared.domain.TimeInterval;
import java.util.Objects;
import java.util.UUID;

/** Un turno tomado: su id (para no chocar consigo mismo al reprogramarlo) y su horario. */
public record BookedTime(UUID bookingId, TimeInterval interval) {

    public BookedTime {
        Objects.requireNonNull(bookingId, "bookingId");
        Objects.requireNonNull(interval, "interval");
    }
}
