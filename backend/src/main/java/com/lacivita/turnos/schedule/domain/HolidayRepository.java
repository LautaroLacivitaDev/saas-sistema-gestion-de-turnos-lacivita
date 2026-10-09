package com.lacivita.turnos.schedule.domain;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface HolidayRepository extends Repository<Holiday, UUID> {

    Holiday saveAndFlush(Holiday holiday);

    void delete(Holiday holiday);

    Optional<Holiday> findById(UUID id);

    List<Holiday> findAllByDateBetweenOrderByDateAsc(LocalDate from, LocalDate to);

    /** {@code true} si ese día no se atiende en la sucursal (feriado propio o de todo el negocio). */
    @Query("""
            select count(h) > 0 from Holiday h
            where h.date = :date and (h.branchId is null or h.branchId = :branchId)
            """)
    boolean isHolidayAt(@Param("branchId") UUID branchId, @Param("date") LocalDate date);

    default Holiday require(UUID id) {
        return findById(id).orElseThrow(HolidayNotFoundException::new);
    }
}
