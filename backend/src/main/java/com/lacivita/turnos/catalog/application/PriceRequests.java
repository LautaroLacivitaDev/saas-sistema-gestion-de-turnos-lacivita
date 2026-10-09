package com.lacivita.turnos.catalog.application;

import com.lacivita.turnos.catalog.application.CatalogViews.OfferingView;
import com.lacivita.turnos.catalog.application.CatalogViews.PriceRequestView;
import com.lacivita.turnos.catalog.domain.BarberService;
import com.lacivita.turnos.catalog.domain.BarberServiceRepository;
import com.lacivita.turnos.catalog.domain.CatalogPolicy;
import com.lacivita.turnos.catalog.domain.NoPendingPriceException;
import com.lacivita.turnos.catalog.domain.Service;
import com.lacivita.turnos.catalog.domain.ServiceRepository;
import com.lacivita.turnos.shared.domain.Money;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import com.lacivita.turnos.users.TeamDirectory;
import com.lacivita.turnos.users.TeamMember;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Precios fuera de rango que pidieron los barberos y esperan la revisión de un gerente o del dueño. */
@Component
public class PriceRequests {

    private final BarberServiceRepository offerings;
    private final ServiceRepository services;
    private final TeamDirectory team;
    private final CatalogActors actors;
    private final ApplicationEventPublisher events;
    private final CatalogMapper mapper;
    private final Clock clock;

    PriceRequests(
            BarberServiceRepository offerings,
            ServiceRepository services,
            TeamDirectory team,
            CatalogActors actors,
            ApplicationEventPublisher events,
            CatalogMapper mapper,
            Clock clock) {
        this.offerings = offerings;
        this.services = services;
        this.team = team;
        this.actors = actors;
        this.events = events;
        this.mapper = mapper;
        this.clock = clock;
    }

    // DECISIÓN: el gerente ve los pedidos de todo el negocio (como ve todo el equipo), pero solo puede
    // resolver los de los barberos de sus sucursales.
    @BusinessScoped
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#businessId, 'Business', 'MANAGER')")
    public Page<PriceRequestView> pending(@BusinessId UUID businessId, Pageable pageable) {
        Page<BarberService> page = offerings.findByBusinessIdAndRequestedPriceIsNotNull(businessId, pageable);
        // Una consulta para los servicios y otra para las personas de la página (sin N+1).
        Map<UUID, Service> servicesById = services
                .findAllByIdIn(page.map(BarberService::getServiceId).toSet())
                .stream()
                .collect(Collectors.toMap(Service::getId, Function.identity()));
        Map<UUID, String> names = team
                .members(businessId, page.map(BarberService::getBarberId).toSet())
                .stream()
                .collect(Collectors.toMap(TeamMember::userId, TeamMember::name));
        return page.map(offering -> toView(offering, servicesById.get(offering.getServiceId()), names));
    }

    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'MANAGER')")
    public OfferingView approve(@BusinessId UUID businessId, AuthenticatedUser actor, UUID offeringId) {
        return review(businessId, actor, offeringId, true);
    }

    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'MANAGER')")
    public OfferingView reject(@BusinessId UUID businessId, AuthenticatedUser actor, UUID offeringId) {
        return review(businessId, actor, offeringId, false);
    }

    private OfferingView review(UUID businessId, AuthenticatedUser actor, UUID offeringId, boolean approved) {
        var offering = offerings.require(offeringId);
        var barber = actors.barberIn(businessId, offering.getBarberId());
        CatalogPolicy.checkCanManageOfferingsOf(
                actor.id(), actors.actorIn(actor, businessId), offering.getBarberId(), barber.membership());

        Money requested = offering.requestedPrice().orElseThrow(NoPendingPriceException::new);
        var now = clock.instant();
        if (approved) {
            offering.approveRequestedPrice(now);
        } else {
            offering.rejectRequestedPrice(now);
        }
        events.publishEvent(new CatalogEvents.PriceReviewed(businessId, offeringId, requested, approved));
        return mapper.toView(offering, services.require(offering.getServiceId()));
    }

    private PriceRequestView toView(BarberService offering, Service service, Map<UUID, String> names) {
        var current = offering.termsFor(service).price();
        var range = service.priceRange();
        return new PriceRequestView(
                offering.getId(),
                offering.getBarberId(),
                names.get(offering.getBarberId()),
                service.getId(),
                service.getName(),
                current.amount(),
                offering.requestedPrice().map(Money::amount).orElse(null),
                range.map(r -> r.min().amount()).orElse(null),
                range.map(r -> r.max().amount()).orElse(null),
                offering.priceRequestedAt().orElse(null));
    }
}
