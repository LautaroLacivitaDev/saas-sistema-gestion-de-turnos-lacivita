package com.lacivita.turnos.schedule.domain;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface OpeningRangeRepository extends Repository<OpeningRange, UUID> {

    List<OpeningRange> saveAll(Iterable<OpeningRange> ranges);

    void flush();

    List<OpeningRange> findAllByBranchIdOrderByWeekdayAscStartsAtAsc(UUID branchId);

    List<OpeningRange> findAllByBranchIdAndWeekday(UUID branchId, DayOfWeek weekday);

    /** Borra enseguida (no al final de la transacción), para poder guardar el horario nuevo después. */
    @Modifying(flushAutomatically = true)
    @Query("delete from OpeningRange r where r.branchId = :branchId")
    void deleteAllOfBranch(@Param("branchId") UUID branchId);
}
