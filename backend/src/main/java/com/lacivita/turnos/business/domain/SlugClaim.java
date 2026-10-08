package com.lacivita.turnos.business.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;

/**
 * Un slug que un negocio usó alguna vez. Queda reservado para siempre a ese negocio, así un link viejo
 * compartido en Instagram o WhatsApp sigue llevando al lugar correcto.
 */
@Entity
@Table(name = "business_slug")
public class SlugClaim {

    @Id
    private String slug;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id")
    private Business business;

    private Instant claimedAt;

    protected SlugClaim() {
        // Requerido por JPA.
    }

    SlugClaim(Slug slug, Business business, Instant now) {
        this.slug = slug.value();
        this.business = Objects.requireNonNull(business, "business");
        this.claimedAt = now;
    }

    public Business getBusiness() {
        return business;
    }

    boolean isFor(Slug candidate) {
        return slug.equals(candidate.value());
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof SlugClaim claim && slug != null && slug.equals(claim.slug));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
