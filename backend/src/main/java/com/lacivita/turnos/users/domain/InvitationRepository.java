package com.lacivita.turnos.users.domain;

import com.lacivita.turnos.shared.domain.Email;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface InvitationRepository extends Repository<Invitation, UUID> {

    Invitation save(Invitation invitation);

    /** Escribe los cambios pendientes; se usa al reemplazar una invitación antes de crear la nueva. */
    void flush();

    Optional<Invitation> findByIdAndBusinessId(UUID id, UUID businessId);

    Optional<Invitation> findByTokenHash(String tokenHash);

    @Query("""
            select i from Invitation i
            where i.businessId = :businessId and i.email = :email
              and i.acceptedAt is null and i.revokedAt is null
            """)
    Optional<Invitation> findOpenByEmail(@Param("businessId") UUID businessId, @Param("email") Email email);

    @Query("""
            select i from Invitation i
            where i.businessId = :businessId
              and i.acceptedAt is null and i.revokedAt is null and i.expiresAt > :now
            """)
    Page<Invitation> findPending(@Param("businessId") UUID businessId, @Param("now") Instant now, Pageable pageable);

    /** Negocio de una invitación. Se consulta antes de saber el negocio, como operación de sistema. */
    @Query("select i.businessId from Invitation i where i.tokenHash = :tokenHash")
    Optional<UUID> findBusinessIdByTokenHash(@Param("tokenHash") String tokenHash);

    default Invitation require(UUID businessId, UUID invitationId) {
        return findByIdAndBusinessId(invitationId, businessId).orElseThrow(InvitationNotFoundException::new);
    }
}
