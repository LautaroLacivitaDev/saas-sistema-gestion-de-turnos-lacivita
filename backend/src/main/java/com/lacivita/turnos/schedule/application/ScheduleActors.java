package com.lacivita.turnos.schedule.application;

import com.lacivita.turnos.business.BranchSummary;
import com.lacivita.turnos.business.BusinessDirectory;
import com.lacivita.turnos.schedule.domain.BarberNotFoundException;
import com.lacivita.turnos.schedule.domain.UnknownBranchException;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.security.BusinessMembership;
import com.lacivita.turnos.shared.security.BusinessRole;
import com.lacivita.turnos.users.TeamDirectory;
import com.lacivita.turnos.users.TeamMember;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/** Con qué rol actúa una persona sobre la agenda, y sobre qué profesional o sucursal. */
@Component
class ScheduleActors {

    /** Un ADMIN que entra como soporte actúa con los permisos del dueño. */
    private static final BusinessMembership SUPPORT = new BusinessMembership(BusinessRole.OWNER, Set.of());

    private final TeamDirectory team;
    private final BusinessDirectory businesses;

    ScheduleActors(TeamDirectory team, BusinessDirectory businesses) {
        this.team = team;
        this.businesses = businesses;
    }

    BusinessMembership actorIn(AuthenticatedUser user, UUID businessId) {
        var member = team.member(businessId, user.id());
        if (member.isPresent()) {
            return member.get().membership();
        }
        if (user.isAdmin()) {
            // El acceso de soporte ya quedó registrado al verificar los permisos del caso de uso.
            return SUPPORT;
        }
        throw new AccessDeniedException("No pertenece al negocio");
    }

    TeamMember barberIn(UUID businessId, UUID barberId) {
        return team.member(businessId, barberId).orElseThrow(BarberNotFoundException::new);
    }

    /** La sucursal, si es de este negocio. Evita escribir datos de un negocio con el id de una sucursal ajena. */
    BranchSummary branchOf(UUID businessId, UUID branchId) {
        return businesses.branch(businessId, branchId).orElseThrow(UnknownBranchException::new);
    }
}
