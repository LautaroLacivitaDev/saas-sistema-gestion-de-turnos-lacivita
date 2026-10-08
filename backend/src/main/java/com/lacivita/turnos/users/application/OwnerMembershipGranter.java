package com.lacivita.turnos.users.application;

import com.lacivita.turnos.business.BusinessRegistered;
import com.lacivita.turnos.users.domain.Membership;
import com.lacivita.turnos.users.domain.MembershipRepository;
import java.time.Clock;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Quien da de alta un negocio queda como dueño. Corre de forma sincrónica dentro de la transacción del
 * alta: si falla, el negocio tampoco se crea.
 */
@Component
class OwnerMembershipGranter {

    private final MembershipRepository memberships;
    private final Clock clock;

    OwnerMembershipGranter(MembershipRepository memberships, Clock clock) {
        this.memberships = memberships;
        this.clock = clock;
    }

    @EventListener
    void on(BusinessRegistered event) {
        memberships.save(Membership.grantOwner(event.ownerUserId(), event.businessId(), clock.instant()));
    }
}
