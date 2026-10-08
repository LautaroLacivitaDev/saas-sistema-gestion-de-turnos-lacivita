package com.lacivita.turnos.users.domain;

import com.lacivita.turnos.shared.security.BusinessMembership;
import com.lacivita.turnos.shared.security.BusinessRole;
import java.util.Set;
import java.util.UUID;

/**
 * Quién puede gestionar a quién dentro del equipo de un negocio.
 *
 * <ul>
 *   <li>El dueño invita gerentes y barberos, cambia roles y gestiona a cualquier miembro salvo a sí mismo.
 *   <li>El gerente solo invita y gestiona barberos, y solo en sus propias sucursales.
 *   <li>El barbero no gestiona el equipo.
 *   <li>Nadie modifica al dueño.
 * </ul>
 *
 * <p>Que la persona pertenezca al negocio lo verifica antes {@code @PreAuthorize}; estas reglas deciden
 * sobre la acción concreta.
 */
public final class TeamPolicy {

    private TeamPolicy() {}

    public static void checkCanInvite(BusinessMembership actor, BusinessRole inviteeRole, Set<UUID> branchIds) {
        switch (actor.role()) {
            case OWNER -> {}
            case MANAGER -> {
                if (inviteeRole != BusinessRole.BARBER) {
                    throw TeamActionNotAllowedException.onlyOwnerManagesManagers();
                }
                requireWithinActorBranches(actor, branchIds);
            }
            case BARBER -> throw TeamActionNotAllowedException.barbersDoNotManageTheTeam();
        }
    }

    /** Dar de baja o editar a un miembro. */
    public static void checkCanManage(BusinessMembership actor, Membership target) {
        if (target.isOwner()) {
            throw TeamActionNotAllowedException.ownerIsFixed();
        }
        switch (actor.role()) {
            case OWNER -> {}
            case MANAGER -> {
                if (target.getRole() != BusinessRole.BARBER) {
                    throw TeamActionNotAllowedException.onlyOwnerManagesManagers();
                }
                requireWithinActorBranches(actor, target.getBranchIds());
            }
            case BARBER -> throw TeamActionNotAllowedException.barbersDoNotManageTheTeam();
        }
    }

    /** Nombrar o quitar gerentes es exclusivo del dueño. */
    public static void checkCanChangeRole(BusinessMembership actor, Membership target) {
        if (target.isOwner()) {
            throw TeamActionNotAllowedException.ownerIsFixed();
        }
        if (actor.role() != BusinessRole.OWNER) {
            throw TeamActionNotAllowedException.onlyOwnerManagesManagers();
        }
    }

    public static void checkCanAssignBranches(BusinessMembership actor, Membership target, Set<UUID> branchIds) {
        checkCanManage(actor, target);
        if (actor.role() == BusinessRole.MANAGER) {
            requireWithinActorBranches(actor, branchIds);
        }
    }

    private static void requireWithinActorBranches(BusinessMembership actor, Set<UUID> branchIds) {
        if (!branchIds.stream().allMatch(actor::covers)) {
            throw TeamActionNotAllowedException.outsideActorBranches();
        }
    }
}
