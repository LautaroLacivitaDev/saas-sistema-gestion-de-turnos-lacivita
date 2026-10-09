package com.lacivita.turnos.schedule.domain;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Reglas de agenda de un negocio. Mientras el dueño no las cambie, rigen las de {@link ScheduleRules#DEFAULT}. */
@Entity
@Table(name = "schedule_settings")
public class ScheduleSettings {

    @Id
    private UUID businessId;

    @Embedded
    private ScheduleRules rules;

    private Instant updatedAt;

    /** Nulo hasta el primer guardado: así Spring Data distingue un alta (el id lo asignamos nosotros). */
    @Version
    private Long version;

    protected ScheduleSettings() {
        // Requerido por JPA.
    }

    private ScheduleSettings(UUID businessId, ScheduleRules rules, Instant now) {
        this.businessId = Objects.requireNonNull(businessId, "businessId");
        this.rules = Objects.requireNonNull(rules, "rules");
        this.updatedAt = now;
    }

    public static ScheduleSettings of(UUID businessId, ScheduleRules rules, Instant now) {
        return new ScheduleSettings(businessId, rules, now);
    }

    public void change(ScheduleRules newRules, Instant now) {
        this.rules = Objects.requireNonNull(newRules, "newRules");
        this.updatedAt = now;
    }

    public ScheduleRules rules() {
        return rules;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || (other instanceof ScheduleSettings settings
                        && businessId != null
                        && businessId.equals(settings.businessId));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
