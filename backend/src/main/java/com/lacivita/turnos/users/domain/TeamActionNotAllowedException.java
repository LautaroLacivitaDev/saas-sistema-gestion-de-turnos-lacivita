package com.lacivita.turnos.users.domain;

import com.lacivita.turnos.shared.domain.ForbiddenException;

/** La persona pertenece al negocio, pero su rol no le permite esta acción sobre el equipo. */
public class TeamActionNotAllowedException extends ForbiddenException {

    private TeamActionNotAllowedException(String code, String message) {
        super(code, message);
    }

    static TeamActionNotAllowedException ownerIsFixed() {
        return new TeamActionNotAllowedException(
                "owner_is_fixed", "El dueño del negocio no se puede modificar ni dar de baja.");
    }

    static TeamActionNotAllowedException onlyOwnerManagesManagers() {
        return new TeamActionNotAllowedException(
                "only_owner_manages_managers", "Solo el dueño puede nombrar, modificar o quitar gerentes.");
    }

    static TeamActionNotAllowedException barbersDoNotManageTheTeam() {
        return new TeamActionNotAllowedException(
                "barbers_do_not_manage_team", "Los barberos no pueden gestionar el equipo.");
    }

    static TeamActionNotAllowedException outsideActorBranches() {
        return new TeamActionNotAllowedException(
                "outside_your_branches", "Solo podés gestionar personas de tus sucursales.");
    }
}
