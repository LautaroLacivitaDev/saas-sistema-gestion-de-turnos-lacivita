package com.lacivita.turnos.notifications.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface DemoEmailRepository extends Repository<DemoEmail, UUID> {

    DemoEmail save(DemoEmail email);

    Optional<DemoEmail> findByIdAndRecipient(UUID id, String recipient);

    /** Los últimos emails de una dirección, del más nuevo al más viejo. */
    List<DemoEmail> findTop20ByRecipientOrderByCreatedAtDesc(String recipient);

    @Modifying
    @Query("delete from DemoEmail e where e.createdAt < :before")
    int deleteOlderThan(@Param("before") Instant before);
}
