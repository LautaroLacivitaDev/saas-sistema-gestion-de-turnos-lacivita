package com.lacivita.turnos.catalog.application;

import com.lacivita.turnos.catalog.application.CatalogViews.ServiceView;
import com.lacivita.turnos.catalog.domain.BarberService;
import com.lacivita.turnos.catalog.domain.BarberServiceRepository;
import com.lacivita.turnos.catalog.domain.PriceRange;
import com.lacivita.turnos.catalog.domain.Service;
import com.lacivita.turnos.catalog.domain.ServiceDetails;
import com.lacivita.turnos.catalog.domain.ServiceRepository;
import com.lacivita.turnos.catalog.domain.ServiceStatus;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import com.lacivita.turnos.users.TeamDirectory;
import java.time.Clock;
import java.util.Set;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicios del catálogo: los crean y editan gerentes y dueño, los barberos proponen nuevos y el dueño
 * fija el rango de precios.
 */
// DECISIÓN: los casos de uso del catálogo se anotan con @Component y no con @Service, porque la entidad
// del glosario se llama Service y chocaría con la anotación de Spring.
@Component
public class ServiceCatalog {

    private final ServiceRepository services;
    private final BarberServiceRepository offerings;
    private final ServiceNames names;
    private final TeamDirectory team;
    private final ApplicationEventPublisher events;
    private final CatalogMapper mapper;
    private final Clock clock;

    ServiceCatalog(
            ServiceRepository services,
            BarberServiceRepository offerings,
            ServiceNames names,
            TeamDirectory team,
            ApplicationEventPublisher events,
            CatalogMapper mapper,
            Clock clock) {
        this.services = services;
        this.offerings = offerings;
        this.names = names;
        this.team = team;
        this.events = events;
        this.mapper = mapper;
        this.clock = clock;
    }

    @BusinessScoped
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#businessId, 'Business', 'BARBER')")
    public Page<ServiceView> list(@BusinessId UUID businessId, Set<ServiceStatus> statuses, Pageable pageable) {
        return services.findByBusinessIdAndStatusIn(businessId, statuses, pageable)
                .map(mapper::toView);
    }

    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'MANAGER')")
    public ServiceView create(@BusinessId UUID businessId, ServiceDetails details) {
        var service = names.saveNew(Service.create(businessId, details, clock.instant()));
        events.publishEvent(new CatalogEvents.ServiceCreated(businessId, service.getId(), details));
        return mapper.toView(service);
    }

    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'MANAGER')")
    public ServiceView update(@BusinessId UUID businessId, UUID serviceId, ServiceDetails details) {
        var service = services.require(serviceId);
        var before = service.details();
        service.update(details, clock.instant());
        names.flushChanges();
        events.publishEvent(new CatalogEvents.ServiceUpdated(businessId, serviceId, before, details));
        return mapper.toView(service);
    }

    /** Fija el rango de precios del servicio, o lo quita con {@code null}. Solo el dueño. */
    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'OWNER')")
    public ServiceView limitPrices(@BusinessId UUID businessId, UUID serviceId, PriceRange range) {
        var service = services.require(serviceId);
        var before = service.priceRange().orElse(null);
        service.limitPrices(range, clock.instant());
        events.publishEvent(new CatalogEvents.PriceRangeChanged(businessId, serviceId, before, range));
        return mapper.toView(service);
    }

    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'MANAGER')")
    public ServiceView deactivate(@BusinessId UUID businessId, UUID serviceId) {
        var service = services.require(serviceId);
        service.deactivate(clock.instant());
        publishStatusChange(businessId, service, ServiceStatus.ACTIVE);
        return mapper.toView(service);
    }

    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'MANAGER')")
    public ServiceView reactivate(@BusinessId UUID businessId, UUID serviceId) {
        var service = services.require(serviceId);
        service.reactivate(clock.instant());
        publishStatusChange(businessId, service, ServiceStatus.INACTIVE);
        return mapper.toView(service);
    }

    /** Un barbero sugiere un servicio que no está en el catálogo. Lo aprueba un gerente. */
    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'BARBER')")
    public ServiceView propose(@BusinessId UUID businessId, AuthenticatedUser proposer, ServiceDetails details) {
        var service = names.saveNew(Service.propose(businessId, details, proposer.id(), clock.instant()));
        events.publishEvent(new CatalogEvents.ServiceProposed(businessId, service.getId(), proposer.id(), details));
        return mapper.toView(service);
    }

    /** Aprueba una propuesta. Quien la propuso pasa a ofrecer el servicio, si sigue en el equipo. */
    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'MANAGER')")
    public ServiceView approve(@BusinessId UUID businessId, UUID serviceId) {
        var service = services.require(serviceId);
        var now = clock.instant();
        service.approve(now);
        publishStatusChange(businessId, service, ServiceStatus.PROPOSED);
        service.proposedBy()
                .filter(proposer -> team.member(businessId, proposer).isPresent())
                .ifPresent(proposer -> offerings.save(BarberService.offer(proposer, service, now)));
        return mapper.toView(service);
    }

    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'MANAGER')")
    public ServiceView reject(@BusinessId UUID businessId, UUID serviceId) {
        var service = services.require(serviceId);
        service.reject(clock.instant());
        publishStatusChange(businessId, service, ServiceStatus.PROPOSED);
        return mapper.toView(service);
    }

    private void publishStatusChange(UUID businessId, Service service, ServiceStatus before) {
        events.publishEvent(
                new CatalogEvents.ServiceStatusChanged(businessId, service.getId(), before, service.getStatus()));
    }
}
