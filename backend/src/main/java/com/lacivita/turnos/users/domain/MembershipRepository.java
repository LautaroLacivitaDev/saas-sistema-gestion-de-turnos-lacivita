package com.lacivita.turnos.users.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.Repository;

public interface MembershipRepository extends Repository<Membership, UUID> {

    Membership save(Membership membership);

    void delete(Membership membership);

    Optional<Membership> findByUserIdAndBusinessId(UUID userId, UUID businessId);

    boolean existsByUserIdAndBusinessId(UUID userId, UUID businessId);

    Page<Membership> findByBusinessId(UUID businessId, Pageable pageable);

    /** Negocios en los que trabaja una persona. Son pocos: se devuelven completos. */
    List<Membership> findAllByUserIdOrderByCreatedAtAsc(UUID userId);

    default Membership requireMember(UUID userId, UUID businessId) {
        return findByUserIdAndBusinessId(userId, businessId).orElseThrow(MemberNotFoundException::new);
    }
}
