package com.lacivita.turnos.schedule.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.lacivita.turnos.shared.domain.TimeInterval;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class SlotFinderTests {

    static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");
    static final LocalDate MONDAY = LocalDate.of(2026, 10, 12);
    static final Instant WEEK_BEFORE =
            MONDAY.minusDays(7).atStartOfDay(BUENOS_AIRES).toInstant();
    static final Duration HALF_HOUR = Duration.ofMinutes(30);

    /** Sin preparación ni anticipación mínima, horarios cada 15 minutos. */
    static final ScheduleRules RULES = new ScheduleRules(0, 0, 60, 15);

    @Nested
    class WorkingHours {

        @Test
        void theBreakBetweenTwoShiftsHasNoSlots() {
            var plan =
                    plan(List.of(range("09:00", "18:00")), List.of(range("09:00", "13:00"), range("14:00", "18:00")));

            var starts = localTimes(SlotFinder.freeStarts(plan, HALF_HOUR, RULES, WEEK_BEFORE), BUENOS_AIRES);

            assertThat(starts)
                    .startsWith("09:00", "09:15")
                    .contains("12:30", "14:00")
                    .endsWith("17:30");
            assertThat(starts).doesNotContain("12:45", "13:00", "13:30", "13:45");
        }

        @Test
        void onlyTheTimeWhenBothTheBarberAndTheBranchWorkCounts() {
            var plan = plan(List.of(range("10:00", "12:00")), List.of(range("08:00", "20:00")));

            var starts = localTimes(SlotFinder.freeStarts(plan, HALF_HOUR, RULES, WEEK_BEFORE), BUENOS_AIRES);

            assertThat(starts).containsExactly("10:00", "10:15", "10:30", "10:45", "11:00", "11:15", "11:30");
        }

        @Test
        void aLongServiceOnlyStartsWhereItFits() {
            var plan = plan(List.of(range("09:00", "11:00")), List.of(range("09:00", "11:00")));

            var starts = SlotFinder.freeStarts(plan, Duration.ofMinutes(90), RULES, WEEK_BEFORE);

            assertThat(localTimes(starts, BUENOS_AIRES)).containsExactly("09:00", "09:15", "09:30");
        }

        @Test
        void aHolidayHasNoSlots() {
            var open = List.of(range("09:00", "18:00"));
            var plan = new DayPlan(MONDAY, BUENOS_AIRES, open, open, true, List.of(), List.of());

            assertThat(SlotFinder.freeStarts(plan, HALF_HOUR, RULES, WEEK_BEFORE))
                    .isEmpty();
        }
    }

    @Nested
    class TakenTime {

        @Test
        void blocksAreSkippedWithoutPreparationTime() {
            var open = List.of(range("09:00", "12:00"));
            var block = interval(MONDAY, "10:00", "11:00");
            var plan = new DayPlan(MONDAY, BUENOS_AIRES, open, open, false, List.of(block), List.of());

            var starts = localTimes(SlotFinder.freeStarts(plan, HALF_HOUR, RULES, WEEK_BEFORE), BUENOS_AIRES);

            assertThat(starts).contains("09:30", "11:00").doesNotContain("09:45", "10:00", "10:45");
        }

        @Test
        void bookedAppointmentsKeepThePreparationTimeOnBothSides() {
            var open = List.of(range("09:00", "12:00"));
            var booked = interval(MONDAY, "10:00", "10:30");
            var plan = new DayPlan(MONDAY, BUENOS_AIRES, open, open, false, List.of(), List.of(booked));
            var withBuffer = new ScheduleRules(10, 0, 60, 15);

            var starts = localTimes(SlotFinder.freeStarts(plan, HALF_HOUR, withBuffer, WEEK_BEFORE), BUENOS_AIRES);

            // 9:15 termina 9:45, antes de los 10 minutos de preparación; 10:45 empieza después.
            assertThat(starts).contains("09:15", "10:45").doesNotContain("09:30", "09:45", "10:00", "10:30");
        }
    }

    @Nested
    class Notice {

        @Test
        void slotsStartAfterTheMinimumNotice() {
            var open = List.of(range("09:00", "12:00"));
            var plan = new DayPlan(MONDAY, BUENOS_AIRES, open, open, false, List.of(), List.of());
            var now =
                    ZonedDateTime.of(MONDAY, LocalTime.of(9, 20), BUENOS_AIRES).toInstant();
            var oneHourNotice = new ScheduleRules(0, 60, 60, 15);

            var starts = localTimes(SlotFinder.freeStarts(plan, HALF_HOUR, oneHourNotice, now), BUENOS_AIRES);

            assertThat(starts).first().isEqualTo("10:30");
        }

        @Test
        void daysBeyondTheMaximumAdvanceHaveNoSlots() {
            var open = List.of(range("09:00", "12:00"));
            var plan = new DayPlan(MONDAY, BUENOS_AIRES, open, open, false, List.of(), List.of());
            var fiveDaysAhead = new ScheduleRules(0, 0, 5, 15);

            assertThat(SlotFinder.freeStarts(plan, HALF_HOUR, fiveDaysAhead, WEEK_BEFORE))
                    .isEmpty();
        }
    }

    /**
     * Argentina no cambia la hora, pero un negocio puede tener una sucursal donde sí. Madrid adelanta el
     * reloj el 29 de marzo de 2026 (de 2:00 a 3:00) y lo atrasa el 25 de octubre (de 3:00 a 2:00).
     */
    @Nested
    class DaylightSavingTime {

        static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
        static final ScheduleRules HOURLY = new ScheduleRules(0, 0, 365, 60);
        static final Instant LONG_BEFORE = Instant.parse("2026-01-01T00:00:00Z");

        @Test
        void whenTheClockJumpsForwardTheMissingHourIsNotOffered() {
            var date = LocalDate.of(2026, 3, 29);
            var open = List.of(range("01:00", "05:00"));
            var plan = new DayPlan(date, MADRID, open, open, false, List.of(), List.of());

            var starts = SlotFinder.freeStarts(plan, Duration.ofHours(1), HOURLY, LONG_BEFORE);

            // De 1:00 a 5:00 de reloj pasan solo tres horas reales.
            assertThat(localTimes(starts, MADRID)).containsExactly("01:00", "03:00", "04:00");
        }

        @Test
        void whenTheClockGoesBackTheRepeatedHourIsOfferedTwice() {
            var date = LocalDate.of(2026, 10, 25);
            var open = List.of(range("01:00", "04:00"));
            var plan = new DayPlan(date, MADRID, open, open, false, List.of(), List.of());

            var starts = SlotFinder.freeStarts(plan, Duration.ofHours(1), HOURLY, LONG_BEFORE);

            // De 1:00 a 4:00 de reloj pasan cuatro horas reales: las 2:00 existen dos veces.
            assertThat(starts).hasSize(4);
            assertThat(localTimes(starts, MADRID)).containsExactly("01:00", "02:00", "02:00", "03:00");
        }
    }

    private static DayPlan plan(List<DayRange> opening, List<DayRange> shifts) {
        return new DayPlan(MONDAY, BUENOS_AIRES, opening, shifts, false, List.of(), List.of());
    }

    private static DayRange range(String start, String end) {
        return DayRange.of(start, end);
    }

    private static TimeInterval interval(LocalDate date, String start, String end) {
        return new TimeInterval(
                ZonedDateTime.of(date, LocalTime.parse(start), BUENOS_AIRES).toInstant(),
                ZonedDateTime.of(date, LocalTime.parse(end), BUENOS_AIRES).toInstant());
    }

    private static List<String> localTimes(List<Instant> starts, ZoneId zone) {
        return starts.stream()
                .map(start -> LocalTime.ofInstant(start, zone).toString())
                .toList();
    }
}
