package com.lacivita.turnos.catalog.application;

import com.lacivita.turnos.catalog.application.CatalogViews.ComboView;
import com.lacivita.turnos.catalog.domain.Combo;
import com.lacivita.turnos.catalog.domain.ComboRepository;
import com.lacivita.turnos.catalog.domain.Service;
import com.lacivita.turnos.catalog.domain.ServiceNotFoundException;
import com.lacivita.turnos.catalog.domain.ServiceRepository;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import java.time.Clock;
import java.util.List;
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

/** Combos de servicios. Los arman gerentes y dueño; los ve todo el equipo. */
@Component
public class Combos {

    private final ComboRepository combos;
    private final ServiceRepository services;
    private final ApplicationEventPublisher events;
    private final CatalogMapper mapper;
    private final Clock clock;

    Combos(
            ComboRepository combos,
            ServiceRepository services,
            ApplicationEventPublisher events,
            CatalogMapper mapper,
            Clock clock) {
        this.combos = combos;
        this.services = services;
        this.events = events;
        this.mapper = mapper;
        this.clock = clock;
    }

    @BusinessScoped
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#businessId, 'Business', 'BARBER')")
    public Page<ComboView> list(@BusinessId UUID businessId, Pageable pageable) {
        return combos.findByBusinessId(businessId, pageable).map(mapper::toView);
    }

    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'MANAGER')")
    public ComboView create(@BusinessId UUID businessId, String name, List<UUID> serviceIds) {
        var combo = combos.save(Combo.compose(businessId, name, servicesInOrder(serviceIds), clock.instant()));
        publishSaved(businessId, combo);
        return mapper.toView(combo);
    }

    /** Cambia el nombre, los servicios y si está disponible. */
    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'MANAGER')")
    public ComboView update(
            @BusinessId UUID businessId, UUID comboId, String name, List<UUID> serviceIds, boolean active) {
        var combo = combos.require(comboId);
        var now = clock.instant();
        combo.recompose(name, servicesInOrder(serviceIds), now);
        if (active) {
            combo.activate(now);
        } else {
            combo.deactivate(now);
        }
        publishSaved(businessId, combo);
        return mapper.toView(combo);
    }

    /** Los servicios en el orden pedido. Con el filtro del negocio, uno de otro negocio no se encuentra. */
    private List<Service> servicesInOrder(List<UUID> serviceIds) {
        Map<UUID, Service> found = services.findAllByIdIn(serviceIds).stream()
                .collect(Collectors.toMap(Service::getId, Function.identity()));
        return serviceIds.stream()
                .map(id -> {
                    var service = found.get(id);
                    if (service == null) {
                        throw new ServiceNotFoundException();
                    }
                    return service;
                })
                .toList();
    }

    private void publishSaved(UUID businessId, Combo combo) {
        events.publishEvent(new CatalogEvents.ComboSaved(
                businessId, combo.getId(), combo.getName(), combo.getServiceIds(), combo.isActive()));
    }
}
