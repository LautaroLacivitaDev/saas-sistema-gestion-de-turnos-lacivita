package com.lacivita.turnos.booking.domain;

import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Políticas de reserva del negocio. Por ahora, el plazo para que el cliente cancele o reprograme. */
@Entity
@Table(name = "booking_settings")
public class BookingSettings {

    @Id
    private UUID businessId;

    @Embedded
    private CancellationPolicy cancellation;

    private Instant updatedAt;

    /** Nulo hasta el primer guardado: así Spring Data distingue un alta (el id lo asignamos nosotros). */
    @Version
    private Long version;

    protected BookingSettings() {
        // Requerido por JPA.
    }

    private BookingSettings(UUID businessId, CancellationPolicy cancellation, Instant now) {
        this.businessId = Objects.requireNonNull(businessId, "businessId");
        this.cancellation = Objects.requireNonNull(cancellation, "cancellation");
        this.updatedAt = now;
    }

    public static BookingSettings of(UUID businessId, CancellationPolicy cancellation, Instant now) {
        return new BookingSettings(businessId, cancellation, now);
    }

    public void change(CancellationPolicy newPolicy, Instant now) {
        this.cancellation = Objects.requireNonNull(newPolicy, "newPolicy");
        this.updatedAt = now;
    }

    public CancellationPolicy cancellation() {
        return cancellation;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || (other instanceof BookingSettings settings
                        && businessId != null
                        && businessId.equals(settings.businessId));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
