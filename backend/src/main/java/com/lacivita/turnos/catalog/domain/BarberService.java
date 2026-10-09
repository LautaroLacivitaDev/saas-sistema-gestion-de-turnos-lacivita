package com.lacivita.turnos.catalog.domain;

import com.lacivita.turnos.shared.domain.Ids;
import com.lacivita.turnos.shared.domain.Money;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

/**
 * Un servicio del catálogo que hace un barbero, con su precio y su duración propios o, si no los fija,
 * los valores base del servicio.
 *
 * <p>Si el barbero elige un precio fuera del rango del servicio, el pedido queda pendiente y mientras
 * tanto sigue rigiendo el precio anterior. Un gerente o el dueño lo aprueba o lo rechaza.
 */
@Entity
@Table(name = "barber_service")
public class BarberService {

    /** Resultado de un cambio de precio. */
    public enum PriceOutcome {
        APPLIED,
        AWAITING_APPROVAL
    }

    @Id
    private UUID id;

    @TenantId
    private UUID businessId;

    private UUID barberId;

    private UUID serviceId;

    /** Precio propio; nulo si hereda el precio base. */
    private Money price;

    /** Duración propia; nula si hereda la duración base. */
    private ServiceDuration duration;

    private boolean active;

    /** Precio fuera de rango que espera aprobación; nulo si no hay pedido. */
    private Money requestedPrice;

    private Instant priceRequestedAt;

    private Instant createdAt;

    private Instant updatedAt;

    /** Nulo hasta el primer guardado: así Spring Data distingue un alta (el id lo asignamos nosotros). */
    @Version
    private Long version;

    protected BarberService() {
        // Requerido por JPA.
    }

    private BarberService(UUID barberId, Service service, Instant now) {
        this.id = Ids.newId();
        this.businessId = service.getBusinessId();
        this.barberId = Objects.requireNonNull(barberId, "barberId");
        this.serviceId = service.getId();
        this.active = true;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** El barbero empieza a ofrecer el servicio con los valores base. */
    public static BarberService offer(UUID barberId, Service service, Instant now) {
        requireOfferable(service);
        return new BarberService(barberId, service, now);
    }

    /** Vuelve a ofrecer un servicio que había dejado. Conserva el precio y la duración que tenía. */
    public void resume(Service service, Instant now) {
        requireSameService(service);
        requireOfferable(service);
        this.active = true;
        this.updatedAt = now;
    }

    public void withdraw(Instant now) {
        this.active = false;
        this.requestedPrice = null;
        this.priceRequestedAt = null;
        this.updatedAt = now;
    }

    /**
     * Cambia el precio y la duración propios ({@code null} vuelve a heredar el valor base).
     *
     * <p>La duración se aplica siempre. El precio se aplica si está dentro del rango del servicio, si el
     * servicio no tiene rango o si lo cambia alguien que aprueba precios (gerente o dueño). Si no, queda
     * pendiente de aprobación y reemplaza cualquier pedido anterior.
     */
    public PriceOutcome changeTerms(
            Service service, Money newPrice, ServiceDuration newDuration, boolean approvesPrices, Instant now) {
        requireSameService(service);
        this.duration = newDuration == null ? null : newDuration.requireOneService();
        this.updatedAt = now;
        boolean withinRange = service.priceRange()
                .map(range -> newPrice == null || range.contains(newPrice))
                .orElse(true);
        if (withinRange || approvesPrices) {
            applyPrice(newPrice);
            return PriceOutcome.APPLIED;
        }
        this.requestedPrice = newPrice;
        this.priceRequestedAt = now;
        return PriceOutcome.AWAITING_APPROVAL;
    }

    public void approveRequestedPrice(Instant now) {
        var approved = requestedPrice().orElseThrow(NoPendingPriceException::new);
        applyPrice(approved);
        this.updatedAt = now;
    }

    public void rejectRequestedPrice(Instant now) {
        requestedPrice().orElseThrow(NoPendingPriceException::new);
        this.requestedPrice = null;
        this.priceRequestedAt = null;
        this.updatedAt = now;
    }

    /** Precio y duración con los que el barbero hace el servicio hoy. */
    public Terms termsFor(Service service) {
        requireSameService(service);
        var base = service.baseTerms();
        return new Terms(price == null ? base.price() : price, duration == null ? base.duration() : duration);
    }

    public Optional<Money> ownPrice() {
        return Optional.ofNullable(price);
    }

    public Optional<ServiceDuration> ownDuration() {
        return Optional.ofNullable(duration);
    }

    public Optional<Money> requestedPrice() {
        return Optional.ofNullable(requestedPrice);
    }

    public Optional<Instant> priceRequestedAt() {
        return Optional.ofNullable(priceRequestedAt);
    }

    public boolean isActive() {
        return active;
    }

    public UUID getId() {
        return id;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public UUID getBarberId() {
        return barberId;
    }

    public UUID getServiceId() {
        return serviceId;
    }

    private void applyPrice(Money newPrice) {
        this.price = newPrice;
        this.requestedPrice = null;
        this.priceRequestedAt = null;
    }

    private void requireSameService(Service service) {
        if (!serviceId.equals(service.getId())) {
            throw new IllegalArgumentException("El servicio " + service.getId() + " no es el de esta oferta");
        }
    }

    private static void requireOfferable(Service service) {
        if (!service.isActive()) {
            throw ServiceStatusException.notOfferable();
        }
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof BarberService offering && id != null && id.equals(offering.id));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
