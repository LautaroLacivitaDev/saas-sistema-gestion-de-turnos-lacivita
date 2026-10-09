package com.lacivita.turnos.booking.application;

import com.lacivita.turnos.booking.domain.BarberNotFoundException;
import com.lacivita.turnos.booking.domain.BarberNotInBranchException;
import com.lacivita.turnos.booking.domain.UnknownBranchException;
import com.lacivita.turnos.business.BranchSummary;
import com.lacivita.turnos.business.BusinessDirectory;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.security.BusinessMembership;
import com.lacivita.turnos.shared.security.BusinessRole;
import com.lacivita.turnos.users.TeamDirectory;
import com.lacivita.turnos.users.TeamMember;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/** Con qué rol actúa una persona sobre las reservas, y en qué sucursal y con qué profesional. */
@Component
class BookingActors {

    /** Un ADMIN que entra como soporte actúa con los permisos del dueño. */
    private static final BusinessMembership SUPPORT = new BusinessMembership(BusinessRole.OWNER, Set.of());

    private final TeamDirectory team;
    private final BusinessDirectory businesses;

    BookingActors(TeamDirectory team, BusinessDirectory businesses) {
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

    /** Profesional del equipo que trabaja en la sucursal. */
    TeamMember barberAt(UUID businessId, UUID barberId, UUID branchId) {
        var barber = team.member(businessId, barberId).orElseThrow(BarberNotFoundException::new);
        if (!barber.membership().covers(branchId)) {
            throw new BarberNotInBranchException();
        }
        return barber;
    }

    BranchSummary branchOf(UUID businessId, UUID branchId) {
        return businesses.branch(businessId, branchId).orElseThrow(UnknownBranchException::new);
    }
}
