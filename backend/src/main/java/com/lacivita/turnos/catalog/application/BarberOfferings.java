package com.lacivita.turnos.catalog.application;

import com.lacivita.turnos.catalog.application.CatalogViews.OfferingView;
import com.lacivita.turnos.catalog.application.CatalogViews.TermsChange;
import com.lacivita.turnos.catalog.domain.BarberService;
import com.lacivita.turnos.catalog.domain.BarberServiceRepository;
import com.lacivita.turnos.catalog.domain.CatalogPolicy;
import com.lacivita.turnos.catalog.domain.Service;
import com.lacivita.turnos.catalog.domain.ServiceDuration;
import com.lacivita.turnos.catalog.domain.ServiceRepository;
import com.lacivita.turnos.shared.domain.Money;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import com.lacivita.turnos.users.MemberLeft;
import java.time.Clock;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Qué servicios hace cada barbero y con qué precio y duración. Cada persona del equipo gestiona los
 * suyos; el gerente, los de los barberos de sus sucursales; el dueño, los de todos.
 */
@Component
public class BarberOfferings {

    private final BarberServiceRepository offerings;
    private final ServiceRepository services;
    private final CatalogActors actors;
    private final ApplicationEventPublisher events;
    private final CatalogMapper mapper;
    private final Clock clock;

    BarberOfferings(
            BarberServiceRepository offerings,
            ServiceRepository services,
            CatalogActors actors,
            ApplicationEventPublisher events,
            CatalogMapper mapper,
            Clock clock) {
        this.offerings = offerings;
        this.services = services;
        this.actors = actors;
        this.events = events;
        this.mapper = mapper;
        this.clock = clock;
    }

    /** Servicios que hace hoy un barbero. Son pocos: se devuelven completos. */
    @BusinessScoped
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#businessId, 'Business', 'BARBER')")
    public List<OfferingView> of(@BusinessId UUID businessId, UUID barberId) {
        actors.barberIn(businessId, barberId);
        List<BarberService> current = offerings.findAllByBarberIdAndActiveTrue(barberId);
        Map<UUID, Service> byId =
                services
                        .findAllByIdIn(current.stream()
                                .map(BarberService::getServiceId)
                                .toList())
                        .stream()
                        .collect(Collectors.toMap(Service::getId, Function.identity()));
        return current.stream()
                .map(offering -> mapper.toView(offering, byId.get(offering.getServiceId())))
                .sorted(Comparator.comparing(OfferingView::serviceName))
                .toList();
    }

    /**
     * Empieza a ofrecer un servicio o cambia su precio y duración propios ({@code null} hereda el valor
     * base). Un precio fuera del rango que pide un barbero queda esperando la aprobación de un gerente.
     */
    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'BARBER')")
    public TermsChange setTerms(
            @BusinessId UUID businessId,
            AuthenticatedUser actor,
            UUID barberId,
            UUID serviceId,
            Money price,
            ServiceDuration duration) {
        var actorMembership = actors.actorIn(actor, businessId);
        var barber = actors.barberIn(businessId, barberId);
        CatalogPolicy.checkCanManageOfferingsOf(actor.id(), actorMembership, barberId, barber.membership());

        var service = services.require(serviceId);
        var now = clock.instant();
        var existing = offerings.findByBarberIdAndServiceId(barberId, serviceId);
        var before = existing.filter(BarberService::isActive)
                .map(BarberOfferings::termsOf)
                .orElse(null);
        var offering = existing.orElseGet(() -> BarberService.offer(barberId, service, now));
        if (!offering.isActive()) {
            offering.resume(service, now);
        }
        var previousPrice = offering.ownPrice().orElse(null);
        var outcome =
                offering.changeTerms(service, price, duration, CatalogPolicy.approvesPrices(actorMembership), now);
        offerings.save(offering);

        events.publishEvent(
                new CatalogEvents.OfferingTermsChanged(businessId, offering.getId(), before, termsOf(offering)));
        if (outcome == BarberService.PriceOutcome.AWAITING_APPROVAL) {
            events.publishEvent(new CatalogEvents.PriceRequested(businessId, offering.getId(), previousPrice, price));
        }
        return new TermsChange(mapper.toView(offering, service), outcome.name());
    }

    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'BARBER')")
    public void withdraw(@BusinessId UUID businessId, AuthenticatedUser actor, UUID barberId, UUID serviceId) {
        var actorMembership = actors.actorIn(actor, businessId);
        var barber = actors.barberIn(businessId, barberId);
        CatalogPolicy.checkCanManageOfferingsOf(actor.id(), actorMembership, barberId, barber.membership());

        var offering = offerings.requireActive(barberId, serviceId);
        offering.withdraw(clock.instant());
        events.publishEvent(new CatalogEvents.OfferingWithdrawn(businessId, offering.getId(), barberId, serviceId));
    }

    /** Quien deja el equipo deja de ofrecer sus servicios. Corre en la misma transacción que la baja. */
    @EventListener
    void on(MemberLeft event) {
        var now = clock.instant();
        for (BarberService offering : offerings.findAllByBarberIdAndActiveTrue(event.userId())) {
            offering.withdraw(now);
            events.publishEvent(new CatalogEvents.OfferingWithdrawn(
                    event.businessId(), offering.getId(), event.userId(), offering.getServiceId()));
        }
    }

    private static CatalogEvents.OfferingTerms termsOf(BarberService offering) {
        return new CatalogEvents.OfferingTerms(
                offering.getBarberId(),
                offering.getServiceId(),
                offering.ownPrice().orElse(null),
                offering.ownDuration().orElse(null));
    }
}
