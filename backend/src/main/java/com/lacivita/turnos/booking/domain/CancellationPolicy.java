package com.lacivita.turnos.booking.domain;

import com.lacivita.turnos.shared.domain.InvalidValueException;
import jakarta.persistence.Embeddable;
import java.time.Duration;
import java.time.Instant;

/**
 * Hasta cuándo el cliente puede cancelar o reprogramar su turno por su cuenta.
 *
 * @param noticeHours horas de anticipación; 0 permite hacerlo hasta el inicio del turno
 */
@Embeddable
public record CancellationPolicy(int noticeHours) {

    static final int MAX_NOTICE_HOURS = 7 * 24;

    // DECISIÓN: mientras el dueño no la cambie, el cliente puede cancelar o reprogramar hasta 2 horas antes.
    public static final CancellationPolicy DEFAULT = new CancellationPolicy(2);

    public CancellationPolicy {
        if (noticeHours < 0 || noticeHours > MAX_NOTICE_HOURS) {
            throw new InvalidValueException(
                    "invalid_cancellation_policy", "El plazo de cancelación va de 0 a 168 horas (una semana).");
        }
    }

    /** {@code true} si todavía se puede cambiar un turno que empieza en {@code start}. */
    public boolean allowsChangeAt(Instant start, Instant now) {
        return !now.isAfter(start.minus(Duration.ofHours(noticeHours)));
    }
}
