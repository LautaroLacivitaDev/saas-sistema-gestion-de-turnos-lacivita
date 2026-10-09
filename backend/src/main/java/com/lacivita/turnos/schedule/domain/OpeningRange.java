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
 * Una franja del horario de atención de una sucursal. El horario se reemplaza entero: las franjas no se
 * editan una por una.
 */
@Entity
@Table(name = "branch_opening")
public class OpeningRange {

    @Id
    private UUID id;

    @TenantId
    private UUID businessId;

    private UUID branchId;

    private DayOfWeek weekday;

    private LocalTime startsAt;

    private LocalTime endsAt;

    protected OpeningRange() {
        // Requerido por JPA.
    }

    private OpeningRange(UUID businessId, UUID branchId, DayOfWeek weekday, DayRange range) {
        this.id = Ids.newId();
        this.businessId = Objects.requireNonNull(businessId, "businessId");
        this.branchId = Objects.requireNonNull(branchId, "branchId");
        this.weekday = Objects.requireNonNull(weekday, "weekday");
        this.startsAt = range.start();
        this.endsAt = range.end();
    }

    /** Las franjas de un horario semanal completo de la sucursal. */
    public static List<OpeningRange> of(UUID businessId, UUID branchId, WeeklyHours hours) {
        return hours.days().entrySet().stream()
                .flatMap(day -> day.getValue().stream()
                        .map(range -> new OpeningRange(businessId, branchId, day.getKey(), range)))
                .toList();
    }

    public DayOfWeek getWeekday() {
        return weekday;
    }

    public DayRange range() {
        return new DayRange(startsAt, endsAt);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof OpeningRange range && id != null && id.equals(range.id));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
