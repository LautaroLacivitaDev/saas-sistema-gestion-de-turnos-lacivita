package com.lacivita.turnos.schedule.application;

import com.lacivita.turnos.schedule.application.ScheduleViews.HolidayView;
import com.lacivita.turnos.schedule.domain.Holiday;
import com.lacivita.turnos.schedule.domain.HolidayAlreadyExistsException;
import com.lacivita.turnos.schedule.domain.HolidayRepository;
import com.lacivita.turnos.schedule.domain.SchedulePolicy;
import com.lacivita.turnos.shared.domain.InvalidValueException;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Feriados, cargados a mano: de una sucursal (los carga su gerente o el dueño) o de todo el negocio (solo
 * el dueño).
 */
@Service
public class Holidays {

    /** Un listado abarca como mucho dos años: los feriados de un negocio son pocos por año. */
    static final int MAX_LISTED_DAYS = 2 * 366;

    private final HolidayRepository holidays;
    private final ScheduleActors actors;
    private final ApplicationEventPublisher events;

    Holidays(HolidayRepository holidays, ScheduleActors actors, ApplicationEventPublisher events) {
        this.holidays = holidays;
        this.actors = actors;
        this.events = events;
    }

    @BusinessScoped
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#businessId, 'Business', 'BARBER')")
    public List<HolidayView> between(@BusinessId UUID businessId, LocalDate from, LocalDate to) {
        if (to.isBefore(from) || from.plusDays(MAX_LISTED_DAYS).isBefore(to)) {
            throw new InvalidValueException("invalid_period", "Elegí un período de hasta dos años.");
        }
        return holidays.findAllByDateBetweenOrderByDateAsc(from, to).stream()
                .map(Holidays::toView)
                .toList();
    }

    /** @param branchId sucursal del feriado; {@code null} para todo el negocio */
    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'MANAGER')")
    public HolidayView add(
            @BusinessId UUID businessId, AuthenticatedUser actor, UUID branchId, LocalDate date, String name) {
        if (branchId != null) {
            actors.branchOf(businessId, branchId);
        }
        SchedulePolicy.checkCanManageBranch(actors.actorIn(actor, businessId), branchId);
        var holiday = branchId == null
                ? Holiday.forBusiness(businessId, date, name)
                : Holiday.forBranch(businessId, branchId, date, name);
        try {
            holidays.saveAndFlush(holiday);
        } catch (DataIntegrityViolationException ex) {
            if (Constraints.violated(ex, Constraints.HOLIDAY_UNIQUE)) {
                throw new HolidayAlreadyExistsException();
            }
            throw ex;
        }
        publish(businessId, holiday, true);
        return toView(holiday);
    }

    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'MANAGER')")
    public void remove(@BusinessId UUID businessId, AuthenticatedUser actor, UUID holidayId) {
        var holiday = holidays.require(holidayId);
        SchedulePolicy.checkCanManageBranch(
                actors.actorIn(actor, businessId), holiday.branchId().orElse(null));
        holidays.delete(holiday);
        publish(businessId, holiday, false);
    }

    private void publish(UUID businessId, Holiday holiday, boolean added) {
        events.publishEvent(new ScheduleEvents.HolidayChanged(
                businessId,
                holiday.getId(),
                holiday.branchId().orElse(null),
                holiday.getDate(),
                holiday.getName(),
                added));
    }

    private static HolidayView toView(Holiday holiday) {
        return new HolidayView(holiday.getId(), holiday.branchId().orElse(null), holiday.getDate(), holiday.getName());
    }
}
