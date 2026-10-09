package com.lacivita.turnos.schedule.domain;

import com.lacivita.turnos.shared.domain.Ids;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

/**
 * Una franja del horario de un profesional en una sucursal. Un profesional puede trabajar en varias
 * sucursales, pero nunca a la misma hora: lo garantiza una restricción de exclusión en la base, también
 * cuando dos personas guardan horarios a la vez.
 */
@Entity
@Table(name = "work_shift")
public class WorkShift {

    @Id
    private UUID id;

    @TenantId
    private UUID businessId;

    private UUID barberId;

    private UUID branchId;

    private DayOfWeek weekday;

    private LocalTime startsAt;

    private LocalTime endsAt;

    protected WorkShift() {
        // Requerido por JPA.
    }

    private WorkShift(UUID businessId, UUID barberId, UUID branchId, DayOfWeek weekday, DayRange range) {
        this.id = Ids.newId();
        this.businessId = Objects.requireNonNull(businessId, "businessId");
        this.barberId = Objects.requireNonNull(barberId, "barberId");
        this.branchId = Objects.requireNonNull(branchId, "branchId");
        this.weekday = Objects.requireNonNull(weekday, "weekday");
        this.startsAt = range.start();
        this.endsAt = range.end();
    }

    /** Las franjas de un horario semanal completo del profesional en la sucursal. */
    public static List<WorkShift> of(UUID businessId, UUID barberId, UUID branchId, WeeklyHours hours) {
        return hours.days().entrySet().stream()
                .flatMap(day -> day.getValue().stream()
                        .map(range -> new WorkShift(businessId, barberId, branchId, day.getKey(), range)))
                .toList();
    }

    public UUID getBarberId() {
        return barberId;
    }

    public UUID getBranchId() {
        return branchId;
    }

    public DayOfWeek getWeekday() {
        return weekday;
    }

    public DayRange range() {
        return new DayRange(startsAt, endsAt);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof WorkShift shift && id != null && id.equals(shift.id));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
