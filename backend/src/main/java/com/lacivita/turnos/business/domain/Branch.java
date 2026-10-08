package com.lacivita.turnos.business.domain;

import com.lacivita.turnos.shared.domain.Ids;
import com.lacivita.turnos.shared.domain.PhoneNumber;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

/**
 * Sucursal de un negocio, con su dirección y su zona horaria. Los horarios de atención y los feriados
 * se cargan en el módulo de agenda.
 */
@Entity
@Table(name = "branch")
public class Branch {

    @Id
    private UUID id;

    @TenantId
    private UUID businessId;

    private String name;

    @Embedded
    private Address address;

    @Embedded
    private Coordinates coordinates;

    private PhoneNumber phone;

    private ZoneId timeZone;

    private Instant createdAt;

    private Instant updatedAt;

    /** Nulo hasta el primer guardado: así Spring Data distingue un alta (el id lo asignamos nosotros). */
    @Version
    private Long version;

    protected Branch() {
        // Requerido por JPA.
    }

    private Branch(UUID businessId, BranchDetails details, Instant now) {
        this.id = Ids.newId();
        this.businessId = Objects.requireNonNull(businessId, "businessId");
        this.createdAt = now;
        apply(details, now);
    }

    public static Branch open(UUID businessId, BranchDetails details, Instant now) {
        return new Branch(businessId, details, now);
    }

    public void update(BranchDetails details, Instant now) {
        apply(details, now);
    }

    public BranchDetails details() {
        return new BranchDetails(name, address, coordinates, phone, timeZone);
    }

    public UUID getId() {
        return id;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    private void apply(BranchDetails details, Instant now) {
        this.name = details.name();
        this.address = details.address();
        this.coordinates = details.coordinates();
        this.phone = details.phone();
        this.timeZone = details.timeZone();
        this.updatedAt = now;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof Branch branch && id != null && id.equals(branch.id));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
