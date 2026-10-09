package com.lacivita.turnos.schedule.application;

import com.lacivita.turnos.business.BranchSummary;
import com.lacivita.turnos.catalog.BookableItem;
import com.lacivita.turnos.catalog.ServiceQuotes;
import com.lacivita.turnos.schedule.BookedTimes;
import com.lacivita.turnos.schedule.application.ScheduleViews.AvailabilityView;
import com.lacivita.turnos.schedule.application.ScheduleViews.SlotBarberView;
import com.lacivita.turnos.schedule.application.ScheduleViews.SlotView;
import com.lacivita.turnos.schedule.domain.DayPlan;
import com.lacivita.turnos.schedule.domain.DayRange;
import com.lacivita.turnos.schedule.domain.HolidayRepository;
import com.lacivita.turnos.schedule.domain.OpeningRange;
import com.lacivita.turnos.schedule.domain.OpeningRangeRepository;
import com.lacivita.turnos.schedule.domain.ScheduleRules;
import com.lacivita.turnos.schedule.domain.ScheduleSettingsRepository;
import com.lacivita.turnos.schedule.domain.SlotFinder;
import com.lacivita.turnos.schedule.domain.TimeBlock;
import com.lacivita.turnos.schedule.domain.TimeBlockRepository;
import com.lacivita.turnos.schedule.domain.WorkShift;
import com.lacivita.turnos.schedule.domain.WorkShiftRepository;
import com.lacivita.turnos.shared.domain.TimeInterval;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import com.lacivita.turnos.users.TeamDirectory;
import com.lacivita.turnos.users.TeamMember;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Horarios libres de un día en una sucursal, para un servicio o combo, con uno o con todos los
 * profesionales ("cualquiera disponible"). Corre dentro del negocio: lo protegen el filtro de Hibernate y
 * Row Level Security.
 */
@Component
class AvailabilityReader {

    private static final DateTimeFormatter LOCAL_TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final OpeningRangeRepository openings;
    private final WorkShiftRepository shifts;
    private final HolidayRepository holidays;
    private final TimeBlockRepository blocks;
    private final ScheduleSettingsRepository settings;
    private final TeamDirectory team;
    private final ServiceQuotes quotes;
    private final List<BookedTimes> bookedTimes;
    private final Clock clock;

    AvailabilityReader(
            OpeningRangeRepository openings,
            WorkShiftRepository shifts,
            HolidayRepository holidays,
            TimeBlockRepository blocks,
            ScheduleSettingsRepository settings,
            TeamDirectory team,
            ServiceQuotes quotes,
            List<BookedTimes> bookedTimes,
            Clock clock) {
        this.openings = openings;
        this.shifts = shifts;
        this.holidays = holidays;
        this.blocks = blocks;
        this.settings = settings;
        this.team = team;
        this.quotes = quotes;
        this.bookedTimes = List.copyOf(bookedTimes);
        this.clock = clock;
    }

    /** @param barberId un profesional en particular; {@code null} para "cualquiera disponible" */
    @BusinessScoped
    @Transactional(readOnly = true)
    public AvailabilityView read(
            @BusinessId UUID businessId, BranchSummary branch, BookableItem item, UUID barberId, LocalDate date) {
        var zone = branch.timeZone();
        var day = new TimeInterval(
                date.atStartOfDay(zone).toInstant(),
                date.plusDays(1).atStartOfDay(zone).toInstant());
        ScheduleRules rules = settings.rulesOf(businessId);
        Instant now = clock.instant();
        List<DayRange> opening = openings.findAllByBranchIdAndWeekday(branch.id(), date.getDayOfWeek()).stream()
                .map(OpeningRange::range)
                .toList();
        boolean holiday = holidays.isHolidayAt(branch.id(), date);

        Map<UUID, List<DayRange>> shiftsByBarber =
                shifts.findAllByBranchIdAndWeekday(branch.id(), date.getDayOfWeek()).stream()
                        .filter(shift -> barberId == null || shift.getBarberId().equals(barberId))
                        .collect(Collectors.groupingBy(
                                WorkShift::getBarberId, Collectors.mapping(WorkShift::range, Collectors.toList())));

        Map<Instant, List<SlotBarberView>> slots = new TreeMap<>();
        for (TeamMember barber : team.members(businessId, shiftsByBarber.keySet())) {
            if (!barber.membership().covers(branch.id())) {
                continue;
            }
            quotes.quote(businessId, barber.userId(), item).ifPresent(quote -> {
                var plan = new DayPlan(
                        date,
                        zone,
                        opening,
                        shiftsByBarber.get(barber.userId()),
                        holiday,
                        blocksAffecting(barber.userId(), branch.id(), day),
                        booked(businessId, barber.userId(), day.widenedBy(rules.buffer())));
                var offer = new SlotBarberView(
                        barber.userId(), barber.name(), quote.price().amount(), (int)
                                quote.duration().toMinutes());
                for (Instant start : SlotFinder.freeStarts(plan, quote.duration(), rules, now)) {
                    slots.computeIfAbsent(start, key -> new ArrayList<>()).add(offer);
                }
            });
        }

        var views = slots.entrySet().stream()
                .map(slot -> new SlotView(
                        slot.getKey(),
                        LocalTime.ofInstant(slot.getKey(), zone).format(LOCAL_TIME),
                        slot.getValue().stream()
                                .sorted(Comparator.comparing(SlotBarberView::barberName))
                                .toList()))
                .toList();
        return new AvailabilityView(date, zone.getId(), views);
    }

    private List<TimeInterval> blocksAffecting(UUID barberId, UUID branchId, TimeInterval day) {
        return blocks.findAffecting(barberId, branchId, day.start(), day.end()).stream()
                .map(TimeBlock::interval)
                .toList();
    }

    private List<TimeInterval> booked(UUID businessId, UUID barberId, TimeInterval period) {
        return bookedTimes.stream()
                .flatMap(source -> source.of(businessId, barberId, period).stream())
                .toList();
    }
}
