package com.lacivita.turnos.schedule.domain;

import com.lacivita.turnos.shared.domain.ForbiddenException;

/** La persona pertenece al negocio, pero su rol no le permite esta acción sobre la agenda. */
public class ScheduleActionNotAllowedException extends ForbiddenException {

    private ScheduleActionNotAllowedException(String code, String message) {
        super(code, message);
    }

    static ScheduleActionNotAllowedException onlyOwnSchedule() {
        return new ScheduleActionNotAllowedException(
                "only_own_schedule", "Solo podés cambiar tu propio horario y tus bloqueos.");
    }

    static ScheduleActionNotAllowedException onlyOwnerManagesManagers() {
        return new ScheduleActionNotAllowedException(
                "only_owner_manages_managers", "Solo el dueño puede cambiar la agenda de un gerente.");
    }

    static ScheduleActionNotAllowedException outsideActorBranches() {
        return new ScheduleActionNotAllowedException(
                "outside_your_branches", "Solo podés gestionar la agenda de tus sucursales.");
    }

    static ScheduleActionNotAllowedException onlyOwnerForWholeBusiness() {
        return new ScheduleActionNotAllowedException(
                "only_owner_whole_business", "Solo el dueño puede cargar feriados para todo el negocio.");
    }
}
