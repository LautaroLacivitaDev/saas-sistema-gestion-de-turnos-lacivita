package com.lacivita.turnos.schedule.domain;

import com.lacivita.turnos.shared.domain.TimeInterval;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;

/**
 * Todo lo que hace falta para saber cuándo atiende un profesional en una sucursal un día dado.
 *
 * @param opening franjas en que atiende la sucursal ese día (hora local)
 * @param shifts franjas en que trabaja el profesional en la sucursal ese día (hora local)
 * @param holiday {@code true} si ese día es feriado en la sucursal
 * @param blocks bloqueos del profesional o de la sucursal
 * @param booked turnos ya tomados del profesional
 */
public record DayPlan(
        LocalDate date,
        ZoneId zone,
        List<DayRange> opening,
        List<DayRange> shifts,
        boolean holiday,
        List<TimeInterval> blocks,
        List<TimeInterval> booked) {

    public DayPlan {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(zone, "zone");
        opening = List.copyOf(opening);
        shifts = List.copyOf(shifts);
        blocks = List.copyOf(blocks);
        booked = List.copyOf(booked);
    }
}
