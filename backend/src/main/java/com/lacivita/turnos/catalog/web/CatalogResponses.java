package com.lacivita.turnos.catalog.web;

import com.lacivita.turnos.catalog.application.CatalogViews.BarberTermsView;
import com.lacivita.turnos.catalog.application.CatalogViews.ComboView;
import com.lacivita.turnos.catalog.application.CatalogViews.OfferingView;
import com.lacivita.turnos.catalog.application.CatalogViews.PriceRequestView;
import com.lacivita.turnos.catalog.application.CatalogViews.PublicCatalogView;
import com.lacivita.turnos.catalog.application.CatalogViews.PublicComboView;
import com.lacivita.turnos.catalog.application.CatalogViews.PublicServiceView;
import com.lacivita.turnos.catalog.application.CatalogViews.ServiceView;
import com.lacivita.turnos.catalog.application.CatalogViews.TermsChange;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Respuestas HTTP del catálogo: son el contrato de la API. */
final class CatalogResponses {

    private CatalogResponses() {}

    record Service(
            UUID id,
            String name,
            String category,
            String description,
            int baseDurationMinutes,
            BigDecimal basePrice,
            BigDecimal priceMin,
            BigDecimal priceMax,
            String status,
            UUID proposedBy) {

        static Service from(ServiceView view) {
            return new Service(
                    view.id(),
                    view.name(),
                    view.category(),
                    view.description(),
                    view.baseDurationMinutes(),
                    view.basePrice(),
                    view.priceMin(),
                    view.priceMax(),
                    view.status(),
                    view.proposedBy());
        }
    }

    record Combo(UUID id, String name, List<UUID> serviceIds, boolean active) {

        static Combo from(ComboView view) {
            return new Combo(view.id(), view.name(), view.serviceIds(), view.active());
        }
    }

    record Offering(
            UUID id,
            UUID barberId,
            UUID serviceId,
            String serviceName,
            BigDecimal ownPrice,
            Integer ownDurationMinutes,
            BigDecimal price,
            int durationMinutes,
            BigDecimal requestedPrice) {

        static Offering from(OfferingView view) {
            return new Offering(
                    view.id(),
                    view.barberId(),
                    view.serviceId(),
                    view.serviceName(),
                    view.ownPrice(),
                    view.ownDurationMinutes(),
                    view.price(),
                    view.durationMinutes(),
                    view.requestedPrice());
        }
    }

    /**
     * @param outcome {@code APPLIED} si el precio ya rige, {@code AWAITING_APPROVAL} si quedó esperando
     *     la aprobación de un gerente
     */
    record OfferingChange(Offering offering, String outcome) {

        static OfferingChange from(TermsChange change) {
            return new OfferingChange(Offering.from(change.offering()), change.outcome());
        }
    }

    record PriceRequest(
            UUID offeringId,
            UUID barberId,
            String barberName,
            UUID serviceId,
            String serviceName,
            BigDecimal currentPrice,
            BigDecimal requestedPrice,
            BigDecimal priceMin,
            BigDecimal priceMax,
            Instant requestedAt) {

        static PriceRequest from(PriceRequestView view) {
            return new PriceRequest(
                    view.offeringId(),
                    view.barberId(),
                    view.barberName(),
                    view.serviceId(),
                    view.serviceName(),
                    view.currentPrice(),
                    view.requestedPrice(),
                    view.priceMin(),
                    view.priceMax(),
                    view.requestedAt());
        }
    }

    record BarberTerms(UUID barberId, String barberName, BigDecimal price, int durationMinutes) {

        static BarberTerms from(BarberTermsView view) {
            return new BarberTerms(view.barberId(), view.barberName(), view.price(), view.durationMinutes());
        }
    }

    record PublicService(
            UUID id,
            String name,
            String category,
            String description,
            BigDecimal fromPrice,
            List<BarberTerms> barbers) {

        static PublicService from(PublicServiceView view) {
            return new PublicService(
                    view.id(),
                    view.name(),
                    view.category(),
                    view.description(),
                    view.fromPrice(),
                    view.barbers().stream().map(BarberTerms::from).toList());
        }
    }

    record PublicCombo(UUID id, String name, List<UUID> serviceIds, BigDecimal fromPrice, List<BarberTerms> barbers) {

        static PublicCombo from(PublicComboView view) {
            return new PublicCombo(
                    view.id(),
                    view.name(),
                    view.serviceIds(),
                    view.fromPrice(),
                    view.barbers().stream().map(BarberTerms::from).toList());
        }
    }

    record PublicCatalog(List<PublicService> services, List<PublicCombo> combos) {

        static PublicCatalog from(PublicCatalogView view) {
            return new PublicCatalog(
                    view.services().stream().map(PublicService::from).toList(),
                    view.combos().stream().map(PublicCombo::from).toList());
        }
    }
}
