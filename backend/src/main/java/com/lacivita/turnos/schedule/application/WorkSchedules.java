package com.lacivita.turnos.schedule.application;

import com.lacivita.turnos.schedule.application.ScheduleViews.BranchHoursView;
import com.lacivita.turnos.schedule.application.ScheduleViews.WeeklyHoursView;
import com.lacivita.turnos.schedule.domain.ScheduleOverlapException;
import com.lacivita.turnos.schedule.domain.SchedulePolicy;
import com.lacivita.turnos.schedule.domain.TimeBlockRepository;
import com.lacivita.turnos.schedule.domain.WeeklyHours;
import com.lacivita.turnos.schedule.domain.WorkShift;
import com.lacivita.turnos.schedule.domain.WorkShiftRepository;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import com.lacivita.turnos.users.MemberLeft;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Horario de cada profesional en cada sucursal donde trabaja. Cada persona carga el suyo; el gerente, el
 * de los barberos de sus sucursales; el dueño, el de todos.
 */
@Service
public class WorkSchedules {

    private final WorkShiftRepository shifts;
    private final TimeBlockRepository blocks;
    private final ScheduleActors actors;
    private final ApplicationEventPublisher events;

    WorkSchedules(
            WorkShiftRepository shifts,
            TimeBlockRepository blocks,
            ScheduleActors actors,
            ApplicationEventPublisher events) {
        this.shifts = shifts;
        this.blocks = blocks;
        this.actors = actors;
        this.events = events;
    }

    /** Horario del profesional en cada una de sus sucursales. */
    @BusinessScoped
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#businessId, 'Business', 'BARBER')")
    public List<BranchHoursView> of(@BusinessId UUID businessId, UUID barberId) {
        actors.barberIn(businessId, barberId);
        return shifts.findAllByBarberIdOrderByWeekdayAscStartsAtAsc(barberId).stream()
                .collect(Collectors.groupingBy(WorkShift::getBranchId))
                .entrySet()
                .stream()
                .map(branch -> new BranchHoursView(
                        branch.getKey(),
                        WeeklyHoursView.of(
                                WeeklyHours.from(branch.getValue(), WorkShift::getWeekday, WorkShift::range))))
                .toList();
    }

    /**
     * Reemplaza el horario semanal del profesional en una sucursal. Si se superpone con su horario en otra
     * sucursal, lo rechaza la base (también si dos personas guardan a la vez).
     */
    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'BARBER')")
    public WeeklyHoursView replace(
            @BusinessId UUID businessId, AuthenticatedUser actor, UUID barberId, UUID branchId, WeeklyHours hours) {
        actors.branchOf(businessId, branchId);
        var barber = actors.barberIn(businessId, barberId);
        SchedulePolicy.checkCanManageBarberAt(
                actor.id(), actors.actorIn(actor, businessId), barberId, barber.membership(), branchId);
        SchedulePolicy.checkWorksAt(barber.membership(), branchId);

        shifts.deleteAllOfBarberAtBranch(barberId, branchId);
        try {
            shifts.saveAll(WorkShift.of(businessId, barberId, branchId, hours));
            shifts.flush();
        } catch (DataIntegrityViolationException ex) {
            if (Constraints.violated(ex, Constraints.WORK_SHIFT_NO_OVERLAP)) {
                throw new ScheduleOverlapException();
            }
            throw ex;
        }
        events.publishEvent(new ScheduleEvents.WorkHoursChanged(businessId, barberId, branchId, hours));
        return WeeklyHoursView.of(hours);
    }

    /** Quien deja el equipo deja de tener horario y bloqueos. Corre en la misma transacción que la baja. */
    @EventListener
    void on(MemberLeft event) {
        shifts.deleteAllOfBarber(event.userId());
        blocks.deleteAllOfBarber(event.userId());
    }
}
