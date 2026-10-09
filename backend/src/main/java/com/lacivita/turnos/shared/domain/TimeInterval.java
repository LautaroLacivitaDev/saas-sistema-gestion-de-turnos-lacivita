package com.lacivita.turnos.shared.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;

/** Intervalo de tiempo absoluto, cerrado al inicio y abierto al final: {@code [start, end)}. */
public record TimeInterval(Instant start, Instant end) {

    public TimeInterval {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (!end.isAfter(start)) {
            throw new InvalidValueException("invalid_interval", "El fin tiene que ser posterior al inicio.");
        }
    }

    public static TimeInterval startingAt(Instant start, Duration length) {
        return new TimeInterval(start, start.plus(length));
    }

    /** {@code true} si comparten algún instante. Dos intervalos seguidos ({@code 9-10} y {@code 10-11}) no se cruzan. */
    public boolean overlaps(TimeInterval other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    public boolean contains(TimeInterval other) {
        return !other.start.isBefore(start) && !other.end.isAfter(end);
    }

    /** El mismo intervalo, agrandado hacia los dos lados. */
    public TimeInterval widenedBy(Duration margin) {
        return new TimeInterval(start.minus(margin), end.plus(margin));
    }

    public Duration length() {
        return Duration.between(start, end);
    }
}
