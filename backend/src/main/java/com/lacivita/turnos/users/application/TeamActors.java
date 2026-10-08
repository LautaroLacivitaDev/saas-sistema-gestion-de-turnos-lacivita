package com.lacivita.turnos.users.application;

import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.security.BusinessMembership;
import com.lacivita.turnos.shared.security.BusinessRole;
import com.lacivita.turnos.users.domain.Membership;
import com.lacivita.turnos.users.domain.MembershipRepository;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/** Con qué rol actúa una persona sobre el equipo de un negocio. */
@Component
class TeamActors {

    /** Un ADMIN que entra como soporte actúa con los permisos del dueño. */
    private static final BusinessMembership SUPPORT = new BusinessMembership(BusinessRole.OWNER, Set.of());

    private final MembershipRepository memberships;

    TeamActors(MembershipRepository memberships) {
        this.memberships = memberships;
    }

    BusinessMembership actorIn(AuthenticatedUser user, UUID businessId) {
        var membership = memberships.findByUserIdAndBusinessId(user.id(), businessId);
        if (membership.isPresent()) {
            return membership.map(Membership::toBusinessMembership).get();
        }
        if (user.isAdmin()) {
            // El acceso de soporte ya quedó registrado al verificar los permisos del caso de uso.
            return SUPPORT;
        }
        throw new AccessDeniedException("No pertenece al negocio");
    }
}
