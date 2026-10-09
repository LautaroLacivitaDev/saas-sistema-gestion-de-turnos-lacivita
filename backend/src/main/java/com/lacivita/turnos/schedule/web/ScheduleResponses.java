package com.lacivita.turnos.schedule.web;

import com.lacivita.turnos.schedule.application.ScheduleViews.AvailabilityView;
import com.lacivita.turnos.schedule.application.ScheduleViews.BranchHoursView;
import com.lacivita.turnos.schedule.application.ScheduleViews.DayHoursView;
import com.lacivita.turnos.schedule.application.ScheduleViews.HolidayView;
import com.lacivita.turnos.schedule.application.ScheduleViews.RangeView;
import com.lacivita.turnos.schedule.application.ScheduleViews.RulesView;
import com.lacivita.turnos.schedule.application.ScheduleViews.SlotBarberView;
import com.lacivita.turnos.schedule.application.ScheduleViews.SlotView;
import com.lacivita.turnos.schedule.application.ScheduleViews.TimeBlockView;
import com.lacivita.turnos.schedule.application.ScheduleViews.WeeklyHoursView;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Respuestas HTTP de la agenda: son el contrato de la API. */
final class ScheduleResponses {

    private ScheduleResponses() {}

    record Range(String start, String end) {

        static Range from(RangeView view) {
            return new Range(view.start(), view.end());
        }
    }

    record Day(DayOfWeek day, List<Range> ranges) {

        static Day from(DayHoursView view) {
            return new Day(view.day(), view.ranges().stream().map(Range::from).toList());
        }
    }

    /** Horario semanal. Los días que no aparecen están cerrados. */
    record WeeklyHours(List<Day> days) {

        static WeeklyHours from(WeeklyHoursView view) {
            return new WeeklyHours(view.days().stream().map(Day::from).toList());
        }
    }

    record BranchHours(UUID branchId, WeeklyHours hours) {

        static BranchHours from(BranchHoursView view) {
            return new BranchHours(view.branchId(), WeeklyHours.from(view.hours()));
        }
    }

    record Holiday(UUID id, UUID branchId, LocalDate date, String name) {

        static Holiday from(HolidayView view) {
            return new Holiday(view.id(), view.branchId(), view.date(), view.name());
        }
    }

    record TimeBlock(UUID id, UUID barberId, UUID branchId, Instant startsAt, Instant endsAt, String reason) {

        static TimeBlock from(TimeBlockView view) {
            return new TimeBlock(
                    view.id(), view.barberId(), view.branchId(), view.startsAt(), view.endsAt(), view.reason());
        }
    }

    record Rules(int bufferMinutes, int minNoticeMinutes, int maxAdvanceDays, int slotStepMinutes) {

        static Rules from(RulesView view) {
            return new Rules(
                    view.bufferMinutes(), view.minNoticeMinutes(), view.maxAdvanceDays(), view.slotStepMinutes());
        }
    }

    record SlotBarber(UUID barberId, String barberName, BigDecimal price, int durationMinutes) {

        static SlotBarber from(SlotBarberView view) {
            return new SlotBarber(view.barberId(), view.barberName(), view.price(), view.durationMinutes());
        }
    }

    record Slot(Instant startsAt, String localTime, List<SlotBarber> barbers) {

        static Slot from(SlotView view) {
            return new Slot(
                    view.startsAt(),
                    view.localTime(),
                    view.barbers().stream().map(SlotBarber::from).toList());
        }
    }

    record Availability(LocalDate date, String timeZone, List<Slot> slots) {

        static Availability from(AvailabilityView view) {
            return new Availability(
                    view.date(),
                    view.timeZone(),
                    view.slots().stream().map(Slot::from).toList());
        }
    }
}
