package com.lacivita.turnos.users.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface MembershipRepository extends Repository<Membership, UUID> {

    Membership save(Membership membership);

    Optional<Membership> findByUserIdAndBusinessId(UUID userId, UUID businessId);
}
