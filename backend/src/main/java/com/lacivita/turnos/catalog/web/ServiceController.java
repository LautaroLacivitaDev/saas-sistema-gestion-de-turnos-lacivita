package com.lacivita.turnos.catalog.web;

import com.lacivita.turnos.catalog.application.ServiceCatalog;
import com.lacivita.turnos.catalog.domain.ServiceStatus;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Catálogo: servicios")
@RestController
@RequestMapping("/api/businesses/{businessId}")
class ServiceController {

    /** Sin filtro se listan todos menos las propuestas rechazadas. */
    private static final Set<ServiceStatus> DEFAULT_STATUSES =
            EnumSet.of(ServiceStatus.ACTIVE, ServiceStatus.INACTIVE, ServiceStatus.PROPOSED);

    private final ServiceCatalog catalog;

    ServiceController(ServiceCatalog catalog) {
        this.catalog = catalog;
    }

    @Operation(summary = "Lista los servicios del catálogo", description = "Todo el equipo.")
    @GetMapping("/services")
    PageResponse<CatalogResponses.Service> list(
            @PathVariable UUID businessId,
            @RequestParam(required = false) Set<ServiceStatus> status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = CatalogPages.DEFAULT_SIZE) int size) {
        var statuses = status == null || status.isEmpty() ? DEFAULT_STATUSES : status;
        var pageable = CatalogPages.of(page, size, Sort.by("category", "name"));
        return PageResponse.of(catalog.list(businessId, statuses, pageable), CatalogResponses.Service::from);
    }

    @Operation(summary = "Agrega un servicio al catálogo", description = "Gerentes y dueño.")
    @PostMapping("/services")
    @ResponseStatus(HttpStatus.CREATED)
    CatalogResponses.Service create(
            @PathVariable UUID businessId, @Valid @RequestBody CatalogRequests.ServiceData body) {
        return CatalogResponses.Service.from(catalog.create(businessId, body.details()));
    }

    @Operation(summary = "Cambia los datos de un servicio", description = "Gerentes y dueño.")
    @PutMapping("/services/{serviceId}")
    CatalogResponses.Service update(
            @PathVariable UUID businessId,
            @PathVariable UUID serviceId,
            @Valid @RequestBody CatalogRequests.ServiceData body) {
        return CatalogResponses.Service.from(catalog.update(businessId, serviceId, body.details()));
    }

    @Operation(
            summary = "Fija el rango de precios de un servicio",
            description = "Solo el dueño. Los barberos eligen su precio dentro del rango; fuera de él, lo aprueba"
                    + " un gerente. Mínimo y máximo vacíos quitan el rango.")
    @PutMapping("/services/{serviceId}/price-range")
    CatalogResponses.Service limitPrices(
            @PathVariable UUID businessId,
            @PathVariable UUID serviceId,
            @RequestBody CatalogRequests.PriceRangeData body) {
        return CatalogResponses.Service.from(catalog.limitPrices(businessId, serviceId, body.range()));
    }

    @Operation(summary = "Retira o vuelve a ofrecer un servicio", description = "Gerentes y dueño.")
    @PutMapping("/services/{serviceId}/active")
    CatalogResponses.Service changeAvailability(
            @PathVariable UUID businessId,
            @PathVariable UUID serviceId,
            @Valid @RequestBody CatalogRequests.ServiceAvailability body) {
        var view =
                body.active() ? catalog.reactivate(businessId, serviceId) : catalog.deactivate(businessId, serviceId);
        return CatalogResponses.Service.from(view);
    }

    @Operation(
            summary = "Propone un servicio nuevo",
            description = "Cualquier persona del equipo. Queda pendiente hasta que lo apruebe un gerente.")
    @PostMapping("/service-proposals")
    @ResponseStatus(HttpStatus.CREATED)
    CatalogResponses.Service propose(
            @PathVariable UUID businessId,
            @Valid @RequestBody CatalogRequests.ServiceData body,
            @AuthenticationPrincipal AuthenticatedUser proposer) {
        return CatalogResponses.Service.from(catalog.propose(businessId, proposer, body.details()));
    }

    @Operation(
            summary = "Aprueba un servicio propuesto",
            description = "Gerentes y dueño. Quien lo propuso pasa a ofrecerlo.")
    @PostMapping("/services/{serviceId}/approve")
    CatalogResponses.Service approve(@PathVariable UUID businessId, @PathVariable UUID serviceId) {
        return CatalogResponses.Service.from(catalog.approve(businessId, serviceId));
    }

    @Operation(summary = "Rechaza un servicio propuesto", description = "Gerentes y dueño.")
    @PostMapping("/services/{serviceId}/reject")
    CatalogResponses.Service reject(@PathVariable UUID businessId, @PathVariable UUID serviceId) {
        return CatalogResponses.Service.from(catalog.reject(businessId, serviceId));
    }
}
