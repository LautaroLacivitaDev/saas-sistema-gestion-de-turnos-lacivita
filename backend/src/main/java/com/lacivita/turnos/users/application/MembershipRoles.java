package com.lacivita.turnos.users.application;

import com.lacivita.turnos.shared.security.BusinessMembership;
import com.lacivita.turnos.shared.security.BusinessMembershipResolver;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import com.lacivita.turnos.users.domain.Membership;
import com.lacivita.turnos.users.domain.MembershipRepository;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Informa a la seguridad el rol y las sucursales de una persona en un negocio. */
@Service
class MembershipRoles implements BusinessMembershipResolver {

    private final MembershipRepository memberships;

    MembershipRoles(MembershipRepository memberships) {
        this.memberships = memberships;
    }

    /** Se lee dentro del negocio consultado: así funciona también fuera de una solicitud HTTP. */
    @Override
    @BusinessScoped
    @Transactional(readOnly = true)
    public Optional<BusinessMembership> membershipOf(UUID userId, @BusinessId UUID businessId) {
        return memberships.findByUserIdAndBusinessId(userId, businessId).map(Membership::toBusinessMembership);
    }
}
