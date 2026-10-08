package com.lacivita.turnos.business.domain;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface BranchRepository extends Repository<Branch, UUID> {

    Branch save(Branch branch);

    Optional<Branch> findByIdAndBusinessId(UUID id, UUID businessId);

    /** Un negocio tiene pocas sucursales: se listan completas, ordenadas por antigüedad. */
    List<Branch> findAllByBusinessIdOrderByCreatedAtAsc(UUID businessId);

    @Query("select b.businessId from Branch b where b.id = :branchId")
    Optional<UUID> findBusinessIdById(@Param("branchId") UUID branchId);

    @Query("select count(b) from Branch b where b.businessId = :businessId and b.id in :branchIds")
    long countByBusinessIdAndIdIn(@Param("businessId") UUID businessId, @Param("branchIds") Collection<UUID> branchIds);

    default Branch require(UUID businessId, UUID branchId) {
        return findByIdAndBusinessId(branchId, businessId).orElseThrow(BranchNotFoundException::new);
    }
}
