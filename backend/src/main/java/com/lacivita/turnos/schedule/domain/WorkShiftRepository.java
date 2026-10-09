package com.lacivita.turnos.schedule.domain;

import java.time.DayOfWeek;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

public interface WorkShiftRepository extends Repository<WorkShift, UUID> {

    List<WorkShift> saveAll(Iterable<WorkShift> shifts);

    void flush();

    /** Horario completo de un profesional, en todas sus sucursales. */
    List<WorkShift> findAllByBarberIdOrderByWeekdayAscStartsAtAsc(UUID barberId);

    /** Quién trabaja en la sucursal ese día de la semana. */
    List<WorkShift> findAllByBranchIdAndWeekday(UUID branchId, DayOfWeek weekday);

    /** Borra enseguida (no al final de la transacción), para poder guardar el horario nuevo después. */
    @Modifying(flushAutomatically = true)
    @Query("delete from WorkShift s where s.barberId = :barberId and s.branchId = :branchId")
    void deleteAllOfBarberAtBranch(@Param("barberId") UUID barberId, @Param("branchId") UUID branchId);

    @Modifying(flushAutomatically = true)
    @Query("delete from WorkShift s where s.barberId = :barberId")
    void deleteAllOfBarber(@Param("barberId") UUID barberId);
}
