package com.lacivita.turnos.business.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Negocio (barbería, centro de estética, etc.). Es la unidad de aislamiento de datos: todo lo que
 * pertenece a un negocio lleva su id.
 *
 * <p>El slug actual se guarda en el negocio; todos los que usó alguna vez quedan en sus {@link
 * SlugClaim}, para redirigir links viejos.
 */
@Entity
@Table(name = "business")
public class Business {

    @Id
    private UUID id;

    private String name;

    private String slug;

    @Enumerated(EnumType.STRING)
    private BusinessCategory category;

    private String description;

    private boolean searchable;

    @OneToMany(mappedBy = "business", cascade = CascadeType.PERSIST)
    private Set<SlugClaim> slugClaims = new HashSet<>();

    private Instant createdAt;

    private Instant updatedAt;

    /** Nulo hasta el primer guardado: así Spring Data distingue un alta (el id lo asignamos nosotros). */
    @Version
    private Long version;

    protected Business() {
        // Requerido por JPA.
    }

    private Business(UUID id, BusinessProfile profile, Slug slug, Instant now) {
        this.id = Objects.requireNonNull(id, "id");
        this.createdAt = now;
        apply(profile, now);
        claim(slug, now);
    }

    /**
     * Alta de un negocio. El id lo genera quien registra, antes de abrir la transacción, porque es el
     * contexto de aislamiento con el que se guarda.
     */
    public static Business register(UUID id, BusinessProfile profile, Slug slug, Instant now) {
        return new Business(id, profile, slug, now);
    }

    public void updateProfile(BusinessProfile profile, Instant now) {
        apply(profile, now);
    }

    /**
     * Cambia el link del negocio. El slug anterior sigue perteneciendo al negocio y redirige al nuevo.
     * Volver a un slug que el negocio ya usó no requiere un registro nuevo. Que el slug no sea de otro
     * negocio lo verifica quien llama (y, en última instancia, la clave primaria de business_slug).
     */
    public void changeSlug(Slug newSlug, Instant now) {
        if (hasSlug(newSlug)) {
            return;
        }
        claim(newSlug, now);
        this.updatedAt = now;
    }

    public boolean hasSlug(Slug candidate) {
        return slug.equals(candidate.value());
    }

    public boolean hasUsed(Slug candidate) {
        return slugClaims.stream().anyMatch(claim -> claim.isFor(candidate));
    }

    public BusinessProfile profile() {
        return new BusinessProfile(name, category, description, searchable);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    /** Slug actual. Se devuelve como texto: ya se validó al elegirlo y no se vuelve a validar al leerlo. */
    public String getSlug() {
        return slug;
    }

    private void apply(BusinessProfile profile, Instant now) {
        this.name = profile.name();
        this.category = profile.category();
        this.description = profile.description();
        this.searchable = profile.searchable();
        this.updatedAt = now;
    }

    private void claim(Slug newSlug, Instant now) {
        if (!hasUsed(newSlug)) {
            slugClaims.add(new SlugClaim(newSlug, this, now));
        }
        this.slug = newSlug.value();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof Business business && id != null && id.equals(business.id));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
