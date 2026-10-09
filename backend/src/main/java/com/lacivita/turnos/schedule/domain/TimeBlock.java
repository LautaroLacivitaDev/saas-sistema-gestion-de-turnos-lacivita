package com.lacivita.turnos.schedule.domain;

import com.lacivita.turnos.shared.domain.Ids;
import com.lacivita.turnos.shared.domain.InvalidValueException;
import com.lacivita.turnos.shared.domain.TimeInterval;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

/**
 * Tiempo en que no se toman turnos: de un profesional (un trámite, vacaciones) o de una sucursal entera
 * (una capacitación, una refacción).
 */
@Entity
@Table(name = "time_block")
public class TimeBlock {

    static final Duration MAX_LENGTH = Duration.ofDays(366);
    static final int MAX_REASON_LENGTH = 120;

    @Id
    private UUID id;

    @TenantId
    private UUID businessId;

    /** Profesional bloqueado; nulo si el bloqueo es de una sucursal. */
    private UUID barberId;

    /** Sucursal bloqueada; nula si el bloqueo es de un profesional. */
    private UUID branchId;

    private Instant startsAt;

    private Instant endsAt;

    private String reason;

    private Instant createdAt;

    protected TimeBlock() {
        // Requerido por JPA.
    }

    private TimeBlock(
            UUID businessId, UUID barberId, UUID branchId, TimeInterval interval, String reason, Instant now) {
        if (interval.length().compareTo(MAX_LENGTH) > 0) {
            throw new InvalidValueException("invalid_time_block", "Un bloqueo puede durar hasta un año.");
        }
        this.id = Ids.newId();
        this.businessId = Objects.requireNonNull(businessId, "businessId");
        this.barberId = barberId;
        this.branchId = branchId;
        this.startsAt = interval.start();
        this.endsAt = interval.end();
        this.reason = validReason(reason);
        this.createdAt = now;
    }

    public static TimeBlock forBarber(
            UUID businessId, UUID barberId, TimeInterval interval, String reason, Instant now) {
        return new TimeBlock(businessId, Objects.requireNonNull(barberId, "barberId"), null, interval, reason, now);
    }

    public static TimeBlock forBranch(
            UUID businessId, UUID branchId, TimeInterval interval, String reason, Instant now) {
        return new TimeBlock(businessId, null, Objects.requireNonNull(branchId, "branchId"), interval, reason, now);
    }

    public TimeInterval interval() {
        return new TimeInterval(startsAt, endsAt);
    }

    public Optional<UUID> barberId() {
        return Optional.ofNullable(barberId);
    }

    public Optional<UUID> branchId() {
        return Optional.ofNullable(branchId);
    }

    public Optional<String> reason() {
        return Optional.ofNullable(reason);
    }

    public UUID getId() {
        return id;
    }

    private static String validReason(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String stripped = value.strip();
        if (stripped.length() > MAX_REASON_LENGTH) {
            throw new InvalidValueException(
                    "invalid_time_block", "El motivo puede tener hasta " + MAX_REASON_LENGTH + " caracteres.");
        }
        return stripped;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof TimeBlock block && id != null && id.equals(block.id));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
