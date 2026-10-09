package com.lacivita.turnos.schedule.domain;

import com.lacivita.turnos.shared.domain.TimeInterval;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Calcula los horarios en que un profesional puede empezar un servicio de cierta duración un día dado.
 *
 * <ol>
 *   <li>Un feriado no tiene horarios.
 *   <li>Se trabaja donde se cruzan el horario del profesional y el de la sucursal.
 *   <li>Los horarios se arman en hora local y se pasan a instantes con la zona de la sucursal. En un cambio
 *       de horario, una hora que no existe se corre hacia adelante y una que se repite toma la primera.
 *   <li>Se descartan los que se cruzan con un bloqueo o con un turno tomado más el tiempo de preparación
 *       (antes y después), y los que no respetan la anticipación mínima o máxima.
 * </ol>
 */
public final class SlotFinder {

    private SlotFinder() {}

    public static List<Instant> freeStarts(DayPlan plan, Duration duration, ScheduleRules rules, Instant now) {
        if (plan.holiday() || isBeyondMaxAdvance(plan, rules, now)) {
            return List.of();
        }
        var earliest = now.plus(rules.minNotice());
        var busy = new ArrayList<>(plan.blocks());
        plan.booked().forEach(taken -> busy.add(taken.widenedBy(rules.buffer())));

        var starts = new ArrayList<Instant>();
        for (TimeInterval window : workingWindows(plan)) {
            for (var start = window.start();
                    !start.plus(duration).isAfter(window.end());
                    start = start.plus(rules.slotStep())) {
                var candidate = TimeInterval.startingAt(start, duration);
                if (!start.isBefore(earliest) && busy.stream().noneMatch(candidate::overlaps)) {
                    starts.add(start);
                }
            }
        }
        return starts.stream().distinct().sorted().toList();
    }

    private static boolean isBeyondMaxAdvance(DayPlan plan, ScheduleRules rules, Instant now) {
        var today = LocalDate.ofInstant(now, plan.zone());
        return plan.date().isAfter(today.plusDays(rules.maxAdvanceDays()));
    }

    private static List<TimeInterval> workingWindows(DayPlan plan) {
        var windows = new ArrayList<TimeInterval>();
        for (DayRange shift : plan.shifts()) {
            for (DayRange opening : plan.opening()) {
                shift.intersect(opening)
                        .flatMap(range -> toInstants(plan, range))
                        .ifPresent(windows::add);
            }
        }
        return windows;
    }

    /** Vacío si la franja entera cae en una hora que no existe (el salto de un cambio de horario). */
    private static Optional<TimeInterval> toInstants(DayPlan plan, DayRange range) {
        var start = at(plan, range.start());
        var end = at(plan, range.end());
        return end.isAfter(start) ? Optional.of(new TimeInterval(start, end)) : Optional.empty();
    }

    private static Instant at(DayPlan plan, LocalTime time) {
        return ZonedDateTime.of(plan.date(), time, plan.zone()).toInstant();
    }
}
