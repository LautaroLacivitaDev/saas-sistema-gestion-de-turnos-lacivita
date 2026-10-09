package com.lacivita.turnos.schedule.domain;

import com.lacivita.turnos.shared.security.BusinessMembership;
import com.lacivita.turnos.shared.security.BusinessRole;
import java.util.UUID;

/**
 * Quién gestiona qué parte de la agenda.
 *
 * <ul>
 *   <li>Cada persona del equipo gestiona su propio horario y sus bloqueos, en las sucursales donde trabaja.
 *   <li>El dueño gestiona todo.
 *   <li>El gerente gestiona los horarios y bloqueos de los barberos de sus sucursales, y el horario,
 *       los feriados y los bloqueos de sus sucursales.
 *   <li>Los feriados de todo el negocio los carga el dueño.
 * </ul>
 */
public final class SchedulePolicy {

    private SchedulePolicy() {}

    public static void checkCanManageBarber(
            UUID actorId, BusinessMembership actor, UUID barberId, BusinessMembership barber) {
        if (actorId.equals(barberId)) {
            return;
        }
        switch (actor.role()) {
            case OWNER -> {}
            case MANAGER -> {
                if (barber.role() != BusinessRole.BARBER) {
                    throw ScheduleActionNotAllowedException.onlyOwnerManagesManagers();
                }
                if (!barber.branchIds().stream().allMatch(actor::covers)) {
                    throw ScheduleActionNotAllowedException.outsideActorBranches();
                }
            }
            case BARBER -> throw ScheduleActionNotAllowedException.onlyOwnSchedule();
        }
    }

    /**
     * El horario de un profesional en una sucursal: al gerente le alcanza con que la sucursal sea suya,
     * aunque el barbero también trabaje en otras.
     */
    public static void checkCanManageBarberAt(
            UUID actorId, BusinessMembership actor, UUID barberId, BusinessMembership barber, UUID branchId) {
        if (actorId.equals(barberId) || actor.role() == BusinessRole.OWNER) {
            return;
        }
        if (actor.role() == BusinessRole.BARBER) {
            throw ScheduleActionNotAllowedException.onlyOwnSchedule();
        }
        if (barber.role() != BusinessRole.BARBER) {
            throw ScheduleActionNotAllowedException.onlyOwnerManagesManagers();
        }
        if (!actor.covers(branchId)) {
            throw ScheduleActionNotAllowedException.outsideActorBranches();
        }
    }

    /** Horario, feriados y bloqueos de una sucursal; sin sucursal, de todo el negocio. */
    public static void checkCanManageBranch(BusinessMembership actor, UUID branchIdOrNull) {
        if (branchIdOrNull == null) {
            if (actor.role() != BusinessRole.OWNER) {
                throw ScheduleActionNotAllowedException.onlyOwnerForWholeBusiness();
            }
            return;
        }
        if (!actor.role().includes(BusinessRole.MANAGER)) {
            throw ScheduleActionNotAllowedException.onlyOwnSchedule();
        }
        if (!actor.covers(branchIdOrNull)) {
            throw ScheduleActionNotAllowedException.outsideActorBranches();
        }
    }

    /** Un profesional solo tiene horario en las sucursales donde trabaja. */
    public static void checkWorksAt(BusinessMembership barber, UUID branchId) {
        if (!barber.covers(branchId)) {
            throw new BarberNotInBranchException();
        }
    }
}
