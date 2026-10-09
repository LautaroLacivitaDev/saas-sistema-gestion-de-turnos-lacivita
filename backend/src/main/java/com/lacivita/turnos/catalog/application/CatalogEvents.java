package com.lacivita.turnos.catalog.application;

import com.lacivita.turnos.catalog.domain.PriceRange;
import com.lacivita.turnos.catalog.domain.ServiceDetails;
import com.lacivita.turnos.catalog.domain.ServiceDuration;
import com.lacivita.turnos.catalog.domain.ServiceStatus;
import com.lacivita.turnos.shared.audit.AuditableEvent;
import com.lacivita.turnos.shared.domain.Money;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Cambios en el catálogo. Por ahora solo los consume el registro de auditoría: la especificación pide
 * guardar quién cambió precios y servicios, con el valor anterior y el nuevo.
 */
final class CatalogEvents {

    private CatalogEvents() {}

    /** Base común: todos los eventos del catálogo pertenecen a un negocio. */
    private interface CatalogEvent extends AuditableEvent {

        UUID businessId();

        @Override
        default Optional<UUID> auditBusinessId() {
            return Optional.of(businessId());
        }
    }

    /** Base de los eventos sobre un servicio del catálogo. */
    private interface ServiceEvent extends CatalogEvent {

        UUID serviceId();

        @Override
        default String auditEntityType() {
            return "Service";
        }

        @Override
        default String auditEntityId() {
            return serviceId().toString();
        }
    }

    /** Base de los eventos sobre lo que ofrece un barbero. */
    private interface OfferingEvent extends CatalogEvent {

        UUID offeringId();

        @Override
        default String auditEntityType() {
            return "BarberService";
        }

        @Override
        default String auditEntityId() {
            return offeringId().toString();
        }
    }

    record ServiceCreated(UUID businessId, UUID serviceId, ServiceDetails details) implements ServiceEvent {

        @Override
        public String auditAction() {
            return "service.created";
        }

        @Override
        public Optional<Object> auditAfter() {
            return Optional.of(details);
        }
    }

    record ServiceProposed(UUID businessId, UUID serviceId, UUID proposerId, ServiceDetails details)
            implements ServiceEvent {

        @Override
        public String auditAction() {
            return "service.proposed";
        }

        @Override
        public Optional<Object> auditAfter() {
            return Optional.of(Map.of("details", details, "proposedBy", proposerId));
        }
    }

    record ServiceUpdated(UUID businessId, UUID serviceId, ServiceDetails before, ServiceDetails after)
            implements ServiceEvent {

        @Override
        public String auditAction() {
            return "service.updated";
        }

        @Override
        public Optional<Object> auditBefore() {
            return Optional.of(before);
        }

        @Override
        public Optional<Object> auditAfter() {
            return Optional.of(after);
        }
    }

    record PriceRangeChanged(UUID businessId, UUID serviceId, PriceRange before, PriceRange after)
            implements ServiceEvent {

        @Override
        public String auditAction() {
            return "service.price_range_changed";
        }

        @Override
        public Optional<Object> auditBefore() {
            return Optional.of(rangeOrNone(before));
        }

        @Override
        public Optional<Object> auditAfter() {
            return Optional.of(rangeOrNone(after));
        }
    }

    record ServiceStatusChanged(UUID businessId, UUID serviceId, ServiceStatus before, ServiceStatus after)
            implements ServiceEvent {

        @Override
        public String auditAction() {
            return "service.status_changed";
        }

        @Override
        public Optional<Object> auditBefore() {
            return Optional.of(Map.of("status", before));
        }

        @Override
        public Optional<Object> auditAfter() {
            return Optional.of(Map.of("status", after));
        }
    }

    record ComboSaved(UUID businessId, UUID comboId, String name, List<UUID> serviceIds, boolean active)
            implements CatalogEvent {

        @Override
        public String auditAction() {
            return "combo.saved";
        }

        @Override
        public String auditEntityType() {
            return "Combo";
        }

        @Override
        public String auditEntityId() {
            return comboId.toString();
        }

        @Override
        public Optional<Object> auditAfter() {
            return Optional.of(Map.of("name", name, "serviceIds", serviceIds, "active", active));
        }
    }

    /** Precio y duración propios de un barbero en un servicio, antes y después del cambio. */
    record OfferingTerms(UUID barberId, UUID serviceId, Money price, ServiceDuration duration) {

        Map<String, Object> asMap() {
            var map = new HashMap<String, Object>();
            map.put("barberId", barberId);
            map.put("serviceId", serviceId);
            map.put("price", price == null ? "base" : price.amount());
            map.put("durationMinutes", duration == null ? "base" : duration.minutes());
            return map;
        }
    }

    record OfferingTermsChanged(UUID businessId, UUID offeringId, OfferingTerms before, OfferingTerms after)
            implements OfferingEvent {

        @Override
        public String auditAction() {
            return "offering.terms_changed";
        }

        @Override
        public Optional<Object> auditBefore() {
            return Optional.ofNullable(before).map(OfferingTerms::asMap);
        }

        @Override
        public Optional<Object> auditAfter() {
            return Optional.of(after.asMap());
        }
    }

    record OfferingWithdrawn(UUID businessId, UUID offeringId, UUID barberId, UUID serviceId) implements OfferingEvent {

        @Override
        public String auditAction() {
            return "offering.withdrawn";
        }

        @Override
        public Optional<Object> auditBefore() {
            return Optional.of(Map.of("barberId", barberId, "serviceId", serviceId));
        }
    }

    record PriceRequested(UUID businessId, UUID offeringId, Money current, Money requested) implements OfferingEvent {

        @Override
        public String auditAction() {
            return "offering.price_requested";
        }

        @Override
        public Optional<Object> auditBefore() {
            return Optional.of(Map.of("price", current == null ? "base" : current.amount()));
        }

        @Override
        public Optional<Object> auditAfter() {
            return Optional.of(Map.of("requestedPrice", requested.amount()));
        }
    }

    record PriceReviewed(UUID businessId, UUID offeringId, Money requested, boolean approved) implements OfferingEvent {

        @Override
        public String auditAction() {
            return approved ? "offering.price_approved" : "offering.price_rejected";
        }

        @Override
        public Optional<Object> auditAfter() {
            return Optional.of(Map.of("requestedPrice", requested.amount()));
        }
    }

    private static Object rangeOrNone(PriceRange range) {
        return range == null
                ? Map.of()
                : Map.of("min", range.min().amount(), "max", range.max().amount());
    }
}
