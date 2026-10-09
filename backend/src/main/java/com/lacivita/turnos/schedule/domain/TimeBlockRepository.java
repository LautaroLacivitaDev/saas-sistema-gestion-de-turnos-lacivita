package com.lacivita.turnos.schedule.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface TimeBlockRepository extends Repository<TimeBlock, UUID> {

    TimeBlock save(TimeBlock block);

    void delete(TimeBlock block);

    Optional<TimeBlock> findById(UUID id);

    /** Bloqueos que se cruzan con el período, para el panel. */
    @Query("select b from TimeBlock b where b.startsAt < :to and b.endsAt > :from")
    Page<TimeBlock> findOverlapping(@Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    /** Bloqueos del profesional o de la sucursal que se cruzan con el período (para la disponibilidad). */
    @Query("""
            select b from TimeBlock b
            where (b.barberId = :barberId or b.branchId = :branchId)
              and b.startsAt < :to and b.endsAt > :from
            """)
    List<TimeBlock> findAffecting(
            @Param("barberId") UUID barberId,
            @Param("branchId") UUID branchId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    @Modifying(flushAutomatically = true)
    @Query("delete from TimeBlock b where b.barberId = :barberId")
    void deleteAllOfBarber(@Param("barberId") UUID barberId);

    default TimeBlock require(UUID id) {
        return findById(id).orElseThrow(TimeBlockNotFoundException::new);
    }
}
