package com.lacivita.turnos.schedule.application;

import com.lacivita.turnos.schedule.application.ScheduleViews.WeeklyHoursView;
import com.lacivita.turnos.schedule.domain.OpeningRange;
import com.lacivita.turnos.schedule.domain.OpeningRangeRepository;
import com.lacivita.turnos.schedule.domain.SchedulePolicy;
import com.lacivita.turnos.schedule.domain.WeeklyHours;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Horario de atención de cada sucursal. Lo cargan el dueño y los gerentes de la sucursal. */
@Service
public class BranchHours {

    private final OpeningRangeRepository openings;
    private final ScheduleActors actors;
    private final ApplicationEventPublisher events;

    BranchHours(OpeningRangeRepository openings, ScheduleActors actors, ApplicationEventPublisher events) {
        this.openings = openings;
        this.actors = actors;
        this.events = events;
    }

    /** No exige permisos: el horario de atención se muestra en la página pública. */
    @BusinessScoped
    @Transactional(readOnly = true)
    public WeeklyHoursView of(@BusinessId UUID businessId, UUID branchId) {
        actors.branchOf(businessId, branchId);
        return WeeklyHoursView.of(current(branchId));
    }

    /** Reemplaza el horario semanal completo de la sucursal. */
    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'MANAGER')")
    public WeeklyHoursView replace(
            @BusinessId UUID businessId, AuthenticatedUser actor, UUID branchId, WeeklyHours hours) {
        actors.branchOf(businessId, branchId);
        SchedulePolicy.checkCanManageBranch(actors.actorIn(actor, businessId), branchId);
        openings.deleteAllOfBranch(branchId);
        openings.saveAll(OpeningRange.of(businessId, branchId, hours));
        openings.flush();
        events.publishEvent(new ScheduleEvents.BranchHoursChanged(businessId, branchId, hours));
        return WeeklyHoursView.of(hours);
    }

    private WeeklyHours current(UUID branchId) {
        return WeeklyHours.from(
                openings.findAllByBranchIdOrderByWeekdayAscStartsAtAsc(branchId),
                OpeningRange::getWeekday,
                OpeningRange::range);
    }
}
