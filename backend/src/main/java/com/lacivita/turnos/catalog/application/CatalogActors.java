package com.lacivita.turnos.catalog.application;

import com.lacivita.turnos.catalog.domain.BarberNotFoundException;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.security.BusinessMembership;
import com.lacivita.turnos.shared.security.BusinessRole;
import com.lacivita.turnos.users.TeamDirectory;
import com.lacivita.turnos.users.TeamMember;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

/** Con qué rol actúa una persona sobre el catálogo, y quién es el barbero sobre el que actúa. */
@Component
class CatalogActors {

    /** Un ADMIN que entra como soporte actúa con los permisos del dueño. */
    private static final BusinessMembership SUPPORT = new BusinessMembership(BusinessRole.OWNER, Set.of());

    private final TeamDirectory team;

    CatalogActors(TeamDirectory team) {
        this.team = team;
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

    /** Persona del equipo que hace (o va a hacer) los servicios. */
    TeamMember barberIn(UUID businessId, UUID barberId) {
        return team.member(businessId, barberId).orElseThrow(BarberNotFoundException::new);
    }
}
