package com.lacivita.turnos.schedule.web;

import com.lacivita.turnos.schedule.domain.DayRange;
import com.lacivita.turnos.schedule.domain.ScheduleRules;
import com.lacivita.turnos.schedule.domain.WeeklyHours;
import com.lacivita.turnos.shared.domain.InvalidValueException;
import com.lacivita.turnos.shared.domain.TimeInterval;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Cuerpos de las solicitudes de la agenda y su traducción a objetos del dominio. */
final class ScheduleRequests {

    private ScheduleRequests() {}

    /** Franja en hora local, con formato {@code HH:mm}. */
    record RangeData(@NotBlank String start, @NotBlank String end) {

        DayRange range() {
            return new DayRange(time(start), time(end));
        }

        private static LocalTime time(String value) {
            try {
                return LocalTime.parse(value.strip());
            } catch (DateTimeParseException ex) {
                throw new InvalidValueException("invalid_time", "Las horas van con formato HH:mm, por ejemplo 09:30.");
            }
        }
    }

    record DayData(@NotNull DayOfWeek day, @NotNull List<@Valid RangeData> ranges) {}

    /** Horario semanal completo. Los días que no aparecen quedan cerrados. */
    record WeeklyHoursData(@NotNull List<@Valid DayData> days) {

        WeeklyHours hours() {
            Map<DayOfWeek, List<DayRange>> byDay = days.stream()
                    .collect(Collectors.groupingBy(
                            DayData::day,
                            Collectors.flatMapping(
                                    day -> day.ranges().stream().map(RangeData::range), Collectors.toList())));
            return new WeeklyHours(byDay);
        }
    }

    /** @param branchId sucursal del feriado; vacío para todo el negocio */
    record HolidayData(
            UUID branchId,
            @NotNull(message = "Elegí el día.") LocalDate date,

            @NotBlank(message = "Ingresá el nombre del feriado.") @Size(max = 80)
            String name) {}

    record BlockData(
            @NotNull(message = "Elegí desde cuándo.") Instant startsAt,
            @NotNull(message = "Elegí hasta cuándo.") Instant endsAt,
            @Size(max = 120) String reason) {

        TimeInterval interval() {
            return new TimeInterval(startsAt, endsAt);
        }
    }

    record RulesData(
            @NotNull Integer bufferMinutes,
            @NotNull Integer minNoticeMinutes,
            @NotNull Integer maxAdvanceDays,
            @NotNull Integer slotStepMinutes) {

        ScheduleRules rules() {
            return new ScheduleRules(bufferMinutes, minNoticeMinutes, maxAdvanceDays, slotStepMinutes);
        }
    }
}
