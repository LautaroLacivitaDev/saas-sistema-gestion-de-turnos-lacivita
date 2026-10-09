package com.lacivita.turnos.notifications.domain;

import com.lacivita.turnos.shared.domain.InvalidValueException;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

/**
 * Cuántas horas antes del turno se le recuerda al cliente. Hasta tres recordatorios, entre 1 hora y una
 * semana antes; una lista vacía los desactiva.
 *
 * @param hoursBefore horas antes del turno, de la más lejana a la más cercana
 */
public record ReminderSchedule(List<Integer> hoursBefore) {

    // Antes que DEFAULT: el constructor los usa al inicializar la clase.
    static final int MAX_REMINDERS = 3;
    static final int MAX_HOURS = 168;

    /** Lo que propone la especificación: un día antes y dos horas antes. */
    public static final ReminderSchedule DEFAULT = new ReminderSchedule(List.of(24, 2));

    public ReminderSchedule {
        if (hoursBefore == null) {
            throw new InvalidValueException("invalid_reminders", "Indicá los recordatorios.");
        }
        if (hoursBefore.size() > MAX_REMINDERS) {
            throw new InvalidValueException("invalid_reminders", "Podés tener hasta 3 recordatorios.");
        }
        for (Integer hours : hoursBefore) {
            if (hours == null || hours < 1 || hours > MAX_HOURS) {
                throw new InvalidValueException(
                        "invalid_reminders", "Cada recordatorio va entre 1 y 168 horas (una semana) antes del turno.");
            }
        }
        if (new HashSet<>(hoursBefore).size() != hoursBefore.size()) {
            throw new InvalidValueException("invalid_reminders", "Hay dos recordatorios a la misma hora.");
        }
        hoursBefore = hoursBefore.stream().sorted(Comparator.reverseOrder()).toList();
    }

    /** Cuándo sale cada recordatorio de un turno, salvo los que ya habrían pasado. */
    public List<Instant> sendTimes(Instant startsAt, Instant now) {
        return hoursBefore.stream()
                .map(hours -> startsAt.minus(Duration.ofHours(hours)))
                .filter(time -> time.isAfter(now))
                .toList();
    }
}
