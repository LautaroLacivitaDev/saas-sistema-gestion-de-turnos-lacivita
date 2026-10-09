package com.lacivita.turnos.schedule.domain;

import com.lacivita.turnos.shared.domain.Ids;
import com.lacivita.turnos.shared.domain.InvalidValueException;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

/** Día en que no se atiende: en una sucursal o, sin sucursal, en todo el negocio. Se cargan a mano. */
@Entity
@Table(name = "holiday")
public class Holiday {

    static final int MAX_NAME_LENGTH = 80;

    @Id
    private UUID id;

    @TenantId
    private UUID businessId;

    /** Nula si el feriado es de todo el negocio. */
    private UUID branchId;

    private LocalDate date;

    private String name;

    protected Holiday() {
        // Requerido por JPA.
    }

    private Holiday(UUID businessId, UUID branchId, LocalDate date, String name) {
        this.id = Ids.newId();
        this.businessId = Objects.requireNonNull(businessId, "businessId");
        this.branchId = branchId;
        this.date = Objects.requireNonNull(date, "date");
        this.name = validName(name);
    }

    public static Holiday forBusiness(UUID businessId, LocalDate date, String name) {
        return new Holiday(businessId, null, date, name);
    }

    public static Holiday forBranch(UUID businessId, UUID branchId, LocalDate date, String name) {
        return new Holiday(businessId, Objects.requireNonNull(branchId, "branchId"), date, name);
    }

    public Optional<UUID> branchId() {
        return Optional.ofNullable(branchId);
    }

    public UUID getId() {
        return id;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getName() {
        return name;
    }

    private static String validName(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidValueException("invalid_holiday_name", "Ingresá el nombre del feriado.");
        }
        String stripped = value.strip();
        if (stripped.length() > MAX_NAME_LENGTH) {
            throw new InvalidValueException(
                    "invalid_holiday_name", "El nombre puede tener hasta " + MAX_NAME_LENGTH + " caracteres.");
        }
        return stripped;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof Holiday holiday && id != null && id.equals(holiday.id));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
