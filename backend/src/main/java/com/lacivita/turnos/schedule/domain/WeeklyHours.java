package com.lacivita.turnos.schedule.domain;

import com.lacivita.turnos.shared.domain.InvalidValueException;
import java.time.DayOfWeek;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Horario semanal: para cada día, sus franjas en hora local. Un día sin franjas está cerrado.
 *
 * <p>El descanso es el hueco entre dos franjas (por ejemplo, 9 a 13 y 14 a 19).
 */
// DECISIÓN: los descansos se expresan como huecos entre franjas, en lugar de una lista aparte. Es la
// misma información con una sola regla (las franjas de un día no se superponen) y sin descansos que
// queden fuera del horario.
public record WeeklyHours(Map<DayOfWeek, List<DayRange>> days) {

    static final int MAX_RANGES_PER_DAY = 6;

    public static final WeeklyHours CLOSED = new WeeklyHours(Map.of());

    public WeeklyHours {
        var normalized = new EnumMap<DayOfWeek, List<DayRange>>(DayOfWeek.class);
        days.forEach((day, ranges) -> {
            if (!ranges.isEmpty()) {
                normalized.put(day, validDay(ranges));
            }
        });
        days = Map.copyOf(normalized);
    }

    /** Arma el horario a partir de franjas sueltas (por ejemplo, las guardadas en la base). */
    public static <T> WeeklyHours from(
            Collection<T> items, Function<T, DayOfWeek> dayOf, Function<T, DayRange> rangeOf) {
        return new WeeklyHours(
                items.stream().collect(Collectors.groupingBy(dayOf, Collectors.mapping(rangeOf, Collectors.toList()))));
    }

    /** Franjas de un día, ordenadas. Vacío si ese día no se trabaja. */
    public List<DayRange> on(DayOfWeek day) {
        return days.getOrDefault(day, List.of());
    }

    private static List<DayRange> validDay(List<DayRange> ranges) {
        if (ranges.size() > MAX_RANGES_PER_DAY) {
            throw new InvalidValueException(
                    "invalid_weekly_hours", "Un día puede tener hasta " + MAX_RANGES_PER_DAY + " franjas.");
        }
        var sorted = ranges.stream().sorted().toList();
        for (int i = 1; i < sorted.size(); i++) {
            if (sorted.get(i - 1).overlaps(sorted.get(i))) {
                throw new InvalidValueException(
                        "invalid_weekly_hours", "Las franjas de un mismo día no se pueden superponer.");
            }
        }
        return sorted;
    }
}
