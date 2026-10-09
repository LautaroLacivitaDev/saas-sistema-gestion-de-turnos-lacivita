package com.lacivita.turnos.notifications.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface AppNoticeRepository extends Repository<AppNotice, UUID> {

    AppNotice save(AppNotice notice);

    Optional<AppNotice> findById(UUID id);

    /** Los avisos de la persona en el negocio, del más nuevo al más viejo. */
    Page<AppNotice> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndReadAtIsNull(UUID userId);

    @Modifying
    @Query("update AppNotice n set n.readAt = :now where n.userId = :userId and n.readAt is null")
    int markAllRead(@Param("userId") UUID userId, @Param("now") Instant now);
}
