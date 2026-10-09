package com.lacivita.turnos.catalog.domain;

import com.lacivita.turnos.shared.domain.Ids;
import com.lacivita.turnos.shared.domain.InvalidValueException;
import com.lacivita.turnos.shared.domain.Money;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import org.hibernate.annotations.TenantId;

/**
 * Servicio del catálogo del negocio, compartido por todas las sucursales. Define el nombre, la categoría y
 * los valores de referencia que hereda cada barbero que no fije los propios.
 *
 * <p>Nace activo cuando lo crea un gerente o propuesto cuando lo sugiere un barbero. Nunca se borra: un
 * servicio retirado sigue existiendo para los turnos y reportes históricos.
 */
@Entity
@Table(name = "service")
public class Service {

    @Id
    private UUID id;

    @TenantId
    private UUID businessId;

    private String name;

    private String category;

    private String description;

    private ServiceDuration baseDuration;

    private Money basePrice;

    @Embedded
    @AttributeOverride(name = "min", column = @Column(name = "price_min"))
    @AttributeOverride(name = "max", column = @Column(name = "price_max"))
    private PriceRange priceRange;

    @Enumerated(EnumType.STRING)
    private ServiceStatus status;

    private UUID proposedBy;

    private Instant createdAt;

    private Instant updatedAt;

    /** Nulo hasta el primer guardado: así Spring Data distingue un alta (el id lo asignamos nosotros). */
    @Version
    private Long version;

    protected Service() {
        // Requerido por JPA.
    }

    private Service(UUID businessId, ServiceDetails details, ServiceStatus status, UUID proposedBy, Instant now) {
        this.id = Ids.newId();
        this.businessId = Objects.requireNonNull(businessId, "businessId");
        this.status = status;
        this.proposedBy = proposedBy;
        this.createdAt = now;
        apply(details, now);
    }

    /** Servicio que agrega al catálogo un gerente o el dueño. */
    public static Service create(UUID businessId, ServiceDetails details, Instant now) {
        return new Service(businessId, details, ServiceStatus.ACTIVE, null, now);
    }

    /** Servicio nuevo que sugiere un barbero. Hasta que un gerente lo apruebe no se puede reservar. */
    public static Service propose(UUID businessId, ServiceDetails details, UUID proposerId, Instant now) {
        return new Service(
                businessId, details, ServiceStatus.PROPOSED, Objects.requireNonNull(proposerId, "proposerId"), now);
    }

    public void update(ServiceDetails details, Instant now) {
        if (status == ServiceStatus.REJECTED) {
            throw ServiceStatusException.rejected();
        }
        requireBasePriceWithin(details.basePrice(), priceRange);
        apply(details, now);
    }

    /**
     * Fija o quita el rango de precios. Los precios que los barberos ya tenían fuera del nuevo rango se
     * conservan; el rango aplica a los cambios futuros.
     */
    public void limitPrices(PriceRange range, Instant now) {
        requireBasePriceWithin(basePrice, range);
        this.priceRange = range;
        this.updatedAt = now;
    }

    public void approve(Instant now) {
        requireStatus(ServiceStatus.PROPOSED, ServiceStatusException::notProposed);
        changeStatus(ServiceStatus.ACTIVE, now);
    }

    public void reject(Instant now) {
        requireStatus(ServiceStatus.PROPOSED, ServiceStatusException::notProposed);
        changeStatus(ServiceStatus.REJECTED, now);
    }

    public void deactivate(Instant now) {
        requireStatus(ServiceStatus.ACTIVE, ServiceStatusException::notActive);
        changeStatus(ServiceStatus.INACTIVE, now);
    }

    public void reactivate(Instant now) {
        requireStatus(ServiceStatus.INACTIVE, ServiceStatusException::notInactive);
        changeStatus(ServiceStatus.ACTIVE, now);
    }

    /** {@code true} si los barberos lo pueden ofrecer y los clientes, reservar. */
    public boolean isActive() {
        return status == ServiceStatus.ACTIVE;
    }

    public Terms baseTerms() {
        return new Terms(basePrice, baseDuration);
    }

    public ServiceDetails details() {
        return new ServiceDetails(name, category, description, baseDuration, basePrice);
    }

    public Optional<PriceRange> priceRange() {
        return Optional.ofNullable(priceRange);
    }

    public Optional<UUID> proposedBy() {
        return Optional.ofNullable(proposedBy);
    }

    public UUID getId() {
        return id;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public String getName() {
        return name;
    }

    public ServiceStatus getStatus() {
        return status;
    }

    private void apply(ServiceDetails details, Instant now) {
        this.name = details.name();
        this.category = details.category();
        this.description = details.description();
        this.baseDuration = details.baseDuration();
        this.basePrice = details.basePrice();
        this.updatedAt = now;
    }

    private void requireStatus(ServiceStatus expected, Supplier<ServiceStatusException> otherwise) {
        if (status != expected) {
            throw otherwise.get();
        }
    }

    private void changeStatus(ServiceStatus newStatus, Instant now) {
        this.status = newStatus;
        this.updatedAt = now;
    }

    private static void requireBasePriceWithin(Money basePrice, PriceRange range) {
        if (range != null && !range.contains(basePrice)) {
            throw new InvalidValueException(
                    "base_price_outside_range", "El precio base tiene que estar dentro del rango de precios.");
        }
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof Service service && id != null && id.equals(service.id));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
