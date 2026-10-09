package com.lacivita.turnos.schedule.application;

import com.lacivita.turnos.schedule.domain.DayRange;
import com.lacivita.turnos.schedule.domain.WeeklyHours;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/** Modelos de lectura de la agenda. Nunca exponen entidades. */
public final class ScheduleViews {

    private ScheduleViews() {}

    /** Franja en hora local, con formato {@code HH:mm}. */
    public record RangeView(String start, String end) {

        static RangeView of(DayRange range) {
            return new RangeView(range.start().toString(), range.end().toString());
        }
    }

    /** Un día de la semana con sus franjas. Los días sin franjas no aparecen. */
    public record DayHoursView(DayOfWeek day, List<RangeView> ranges) {}

    public record WeeklyHoursView(List<DayHoursView> days) {

        static WeeklyHoursView of(WeeklyHours hours) {
            return new WeeklyHoursView(Arrays.stream(DayOfWeek.values())
                    .filter(day -> !hours.on(day).isEmpty())
                    .map(day -> new DayHoursView(
                            day, hours.on(day).stream().map(RangeView::of).toList()))
                    .toList());
        }
    }

    /** Horario de un profesional en una de sus sucursales. */
    public record BranchHoursView(UUID branchId, WeeklyHoursView hours) {}

    public record HolidayView(UUID id, UUID branchId, LocalDate date, String name) {}

    public record TimeBlockView(
            UUID id, UUID barberId, UUID branchId, Instant startsAt, Instant endsAt, String reason) {}

    public record RulesView(int bufferMinutes, int minNoticeMinutes, int maxAdvanceDays, int slotStepMinutes) {}

    /** Un profesional libre en un horario, con su precio y su duración para lo que se reserva. */
    public record SlotBarberView(UUID barberId, String barberName, BigDecimal price, int durationMinutes) {}

    /**
     * Horario disponible.
     *
     * @param localTime hora en la zona de la sucursal, con formato {@code HH:mm}
     */
    public record SlotView(Instant startsAt, String localTime, List<SlotBarberView> barbers) {}

    public record AvailabilityView(LocalDate date, String timeZone, List<SlotView> slots) {}
}
