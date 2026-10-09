package com.lacivita.turnos.booking.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.TenantId;
import org.springframework.data.domain.Persistable;

/**
 * Link de un email para que el cliente gestione su turno sin iniciar sesión. Cada email lleva el suyo, así
 * que un turno puede tener varios y todos siguen sirviendo. Solo se guarda el hash del {@link ManageToken}.
 */
@Entity
@Table(name = "appointment_link")
public class AppointmentLink implements Persistable<String> {

    @Id
    private String tokenHash;

    @TenantId
    private UUID businessId;

    private UUID appointmentId;

    private Instant createdAt;

    /** Un link no cambia nunca: se distingue el alta sin columna de versión. */
    @Transient
    private boolean isNew = true;

    protected AppointmentLink() {
        // Requerido por JPA.
    }

    private AppointmentLink(String tokenHash, UUID businessId, UUID appointmentId, Instant now) {
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash");
        this.businessId = Objects.requireNonNull(businessId, "businessId");
        this.appointmentId = Objects.requireNonNull(appointmentId, "appointmentId");
        this.createdAt = Objects.requireNonNull(now, "now");
    }

    /** Link nuevo para el turno. El token en claro solo vive en el email. */
    public static AppointmentLink to(Appointment appointment, ManageToken token, Instant now) {
        return new AppointmentLink(token.hash(), appointment.getBusinessId(), appointment.getId(), now);
    }

    public UUID appointmentId() {
        return appointmentId;
    }

    @Override
    public String getId() {
        return tokenHash;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostLoad
    @PostPersist
    void markStored() {
        this.isNew = false;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || (other instanceof AppointmentLink link && tokenHash != null && tokenHash.equals(link.tokenHash));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
