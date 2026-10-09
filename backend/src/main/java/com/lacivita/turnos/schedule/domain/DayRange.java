package com.lacivita.turnos.schedule.domain;

import com.lacivita.turnos.shared.domain.InvalidValueException;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Optional;

/**
 * Franja horaria dentro de un día, en la hora local de la sucursal: {@code [start, end)}. Va de a 5
 * minutos y no cruza la medianoche.
 */
public record DayRange(LocalTime start, LocalTime end) implements Comparable<DayRange> {

    static final int STEP_MINUTES = 5;

    public DayRange {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (!onStep(start) || !onStep(end)) {
            throw new InvalidValueException(
                    "invalid_time_range", "Los horarios van de a " + STEP_MINUTES + " minutos (9:00, 9:05...).");
        }
        if (!end.isAfter(start)) {
            throw new InvalidValueException(
                    "invalid_time_range", "Cada franja tiene que terminar después de empezar, el mismo día.");
        }
    }

    public static DayRange of(String start, String end) {
        return new DayRange(LocalTime.parse(start), LocalTime.parse(end));
    }

    public boolean overlaps(DayRange other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    /** La parte en común con otra franja, si la hay. */
    public Optional<DayRange> intersect(DayRange other) {
        var from = start.isAfter(other.start) ? start : other.start;
        var to = end.isBefore(other.end) ? end : other.end;
        return to.isAfter(from) ? Optional.of(new DayRange(from, to)) : Optional.empty();
    }

    @Override
    public int compareTo(DayRange other) {
        return start.compareTo(other.start);
    }

    private static boolean onStep(LocalTime time) {
        return time.getSecond() == 0 && time.getNano() == 0 && time.getMinute() % STEP_MINUTES == 0;
    }
}
