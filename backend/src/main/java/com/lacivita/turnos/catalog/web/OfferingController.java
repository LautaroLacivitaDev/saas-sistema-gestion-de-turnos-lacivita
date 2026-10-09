package com.lacivita.turnos.catalog.web;

import com.lacivita.turnos.catalog.application.BarberOfferings;
import com.lacivita.turnos.catalog.application.PriceRequests;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Catálogo: servicios por profesional")
@RestController
@RequestMapping("/api/businesses/{businessId}")
class OfferingController {

    private final BarberOfferings offerings;
    private final PriceRequests priceRequests;

    OfferingController(BarberOfferings offerings, PriceRequests priceRequests) {
        this.offerings = offerings;
        this.priceRequests = priceRequests;
    }

    @Operation(summary = "Servicios que hace un profesional, con su precio y duración", description = "Todo el equipo.")
    @GetMapping("/barbers/{barberId}/services")
    List<CatalogResponses.Offering> of(@PathVariable UUID businessId, @PathVariable UUID barberId) {
        return offerings.of(businessId, barberId).stream()
                .map(CatalogResponses.Offering::from)
                .toList();
    }

    @Operation(
            summary = "Ofrece un servicio o cambia su precio y duración propios",
            description = "Cada persona, los suyos; el gerente, los de los barberos de sus sucursales; el dueño, los"
                    + " de todos. Vacíos heredan los valores base. Si un barbero elige un precio fuera del rango,"
                    + " queda esperando aprobación (outcome AWAITING_APPROVAL).")
    @PutMapping("/barbers/{barberId}/services/{serviceId}")
    CatalogResponses.OfferingChange setTerms(
            @PathVariable UUID businessId,
            @PathVariable UUID barberId,
            @PathVariable UUID serviceId,
            @RequestBody CatalogRequests.OfferingTerms body,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return CatalogResponses.OfferingChange.from(
                offerings.setTerms(businessId, actor, barberId, serviceId, body.ownPrice(), body.ownDuration()));
    }

    @Operation(summary = "Deja de ofrecer un servicio")
    @DeleteMapping("/barbers/{barberId}/services/{serviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void withdraw(
            @PathVariable UUID businessId,
            @PathVariable UUID barberId,
            @PathVariable UUID serviceId,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        offerings.withdraw(businessId, actor, barberId, serviceId);
    }

    @Operation(summary = "Precios fuera de rango que esperan aprobación", description = "Gerentes y dueño.")
    @GetMapping("/price-requests")
    PageResponse<CatalogResponses.PriceRequest> pending(
            @PathVariable UUID businessId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = CatalogPages.DEFAULT_SIZE) int size) {
        var pageable = CatalogPages.of(page, size, Sort.by("priceRequestedAt"));
        return PageResponse.of(priceRequests.pending(businessId, pageable), CatalogResponses.PriceRequest::from);
    }

    @Operation(summary = "Aprueba un precio fuera de rango", description = "Gerentes (de sus sucursales) y dueño.")
    @PostMapping("/price-requests/{offeringId}/approve")
    CatalogResponses.Offering approve(
            @PathVariable UUID businessId,
            @PathVariable UUID offeringId,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return CatalogResponses.Offering.from(priceRequests.approve(businessId, actor, offeringId));
    }

    @Operation(summary = "Rechaza un precio fuera de rango", description = "Gerentes (de sus sucursales) y dueño.")
    @PostMapping("/price-requests/{offeringId}/reject")
    CatalogResponses.Offering reject(
            @PathVariable UUID businessId,
            @PathVariable UUID offeringId,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return CatalogResponses.Offering.from(priceRequests.reject(businessId, actor, offeringId));
    }
}
