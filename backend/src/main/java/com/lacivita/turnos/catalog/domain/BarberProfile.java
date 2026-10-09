package com.lacivita.turnos.catalog.domain;

import com.lacivita.turnos.shared.domain.Ids;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.TenantId;
import org.hibernate.type.SqlTypes;

/** Perfil público de un profesional en un negocio. Si trabaja en dos negocios, tiene uno en cada uno. */
@Entity
@Table(name = "barber_profile")
public class BarberProfile {

    @Id
    private UUID id;

    @TenantId
    private UUID businessId;

    private UUID barberId;

    private String bio;

    @JdbcTypeCode(SqlTypes.ARRAY)
    private List<String> specialties = new ArrayList<>();

    private String photoUrl;

    private Instant updatedAt;

    @Version
    private Long version;

    protected BarberProfile() {
        // Requerido por JPA.
    }

    private BarberProfile(UUID businessId, UUID barberId, ProfessionalProfile profile, Instant now) {
        this.id = Ids.newId();
        this.businessId = Objects.requireNonNull(businessId, "businessId");
        this.barberId = Objects.requireNonNull(barberId, "barberId");
        change(profile, now);
    }

    public static BarberProfile of(UUID businessId, UUID barberId, ProfessionalProfile profile, Instant now) {
        return new BarberProfile(businessId, barberId, profile, now);
    }

    public void change(ProfessionalProfile profile, Instant now) {
        this.bio = profile.bio();
        this.specialties = new ArrayList<>(profile.specialties());
        this.photoUrl = profile.photoUrl();
        this.updatedAt = now;
    }

    public ProfessionalProfile profile() {
        return new ProfessionalProfile(bio, List.copyOf(specialties), photoUrl);
    }

    public UUID getBarberId() {
        return barberId;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof BarberProfile profile && id != null && id.equals(profile.id));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
