package com.lacivita.turnos.catalog.domain;

import com.lacivita.turnos.shared.domain.Ids;
import com.lacivita.turnos.shared.domain.InvalidValueException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

/**
 * Servicios que se reservan juntos (por ejemplo, corte y barba). Los hace un solo profesional, uno detrás
 * del otro: el precio y la duración son la suma de los de ese profesional para cada servicio.
 */
@Entity
@Table(name = "combo")
public class Combo {

    static final int MIN_SERVICES = 2;
    static final int MAX_SERVICES = 5;
    static final int MAX_NAME_LENGTH = 80;

    @Id
    private UUID id;

    @TenantId
    private UUID businessId;

    private String name;

    /** Servicios en el orden en que se hacen. */
    @ElementCollection
    @CollectionTable(name = "combo_service", joinColumns = @JoinColumn(name = "combo_id"))
    @OrderColumn(name = "position")
    @Column(name = "service_id")
    private List<UUID> serviceIds = new ArrayList<>();

    private boolean active;

    private Instant createdAt;

    private Instant updatedAt;

    /** Nulo hasta el primer guardado: así Spring Data distingue un alta (el id lo asignamos nosotros). */
    @Version
    private Long version;

    protected Combo() {
        // Requerido por JPA.
    }

    private Combo(UUID businessId, Instant now) {
        this.id = Ids.newId();
        this.businessId = Objects.requireNonNull(businessId, "businessId");
        this.active = true;
        this.createdAt = now;
    }

    /** Arma un combo con servicios activos del catálogo del negocio. */
    public static Combo compose(UUID businessId, String name, List<Service> services, Instant now) {
        var combo = new Combo(businessId, now);
        combo.recompose(name, services, now);
        return combo;
    }

    /** Cambia el nombre y los servicios. Todos tienen que ser servicios activos del mismo negocio. */
    public void recompose(String newName, List<Service> services, Instant now) {
        this.name = validName(newName);
        this.serviceIds = new ArrayList<>(validServices(services));
        this.updatedAt = now;
    }

    public void activate(Instant now) {
        this.active = true;
        this.updatedAt = now;
    }

    public void deactivate(Instant now) {
        this.active = false;
        this.updatedAt = now;
    }

    /**
     * Precio y duración del combo para un profesional, a partir de sus condiciones en cada servicio.
     * Vacío si no hace alguno de los servicios del combo.
     */
    public Optional<Terms> termsFrom(Map<UUID, Terms> barberTermsByService) {
        Terms total = null;
        for (UUID serviceId : serviceIds) {
            var terms = barberTermsByService.get(serviceId);
            if (terms == null) {
                return Optional.empty();
            }
            total = total == null ? terms : total.plus(terms);
        }
        return Optional.ofNullable(total);
    }

    public List<UUID> getServiceIds() {
        return List.copyOf(serviceIds);
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

    public String getName() {
        return name;
    }

    private static String validName(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidValueException("invalid_combo_name", "Ingresá el nombre del combo.");
        }
        String stripped = value.strip();
        if (stripped.length() > MAX_NAME_LENGTH) {
            throw new InvalidValueException(
                    "invalid_combo_name", "El nombre puede tener hasta " + MAX_NAME_LENGTH + " caracteres.");
        }
        return stripped;
    }

    private List<UUID> validServices(List<Service> services) {
        if (services.size() < MIN_SERVICES || services.size() > MAX_SERVICES) {
            throw new InvalidValueException(
                    "invalid_combo_services",
                    "Un combo lleva entre " + MIN_SERVICES + " y " + MAX_SERVICES + " servicios.");
        }
        var ids = services.stream().map(Service::getId).toList();
        if (new HashSet<>(ids).size() != ids.size()) {
            throw new InvalidValueException("invalid_combo_services", "Un combo no puede repetir un servicio.");
        }
        for (Service service : services) {
            if (!service.getBusinessId().equals(businessId) || !service.isActive()) {
                throw ServiceStatusException.notOfferable();
            }
        }
        return ids;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof Combo combo && id != null && id.equals(combo.id));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
