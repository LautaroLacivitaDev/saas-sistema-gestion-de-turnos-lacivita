package com.lacivita.turnos.users.application;

import com.lacivita.turnos.shared.security.BusinessRole;
import com.lacivita.turnos.shared.security.BusinessRoleResolver;
import com.lacivita.turnos.users.domain.Membership;
import com.lacivita.turnos.users.domain.MembershipRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Informa a la seguridad el rol de una persona en un negocio, a partir de su membresía. */
@Service
class MembershipRoles implements BusinessRoleResolver {

    private final MembershipRepository memberships;

    MembershipRoles(MembershipRepository memberships) {
        this.memberships = memberships;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BusinessRole> roleOf(UUID userId, UUID businessId) {
        return memberships.findByUserIdAndBusinessId(userId, businessId).map(Membership::getRole);
    }
}
