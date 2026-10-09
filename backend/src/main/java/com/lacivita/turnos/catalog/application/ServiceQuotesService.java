package com.lacivita.turnos.catalog.application;

import com.lacivita.turnos.catalog.BookableItem;
import com.lacivita.turnos.catalog.Quote;
import com.lacivita.turnos.catalog.ServiceQuotes;
import com.lacivita.turnos.catalog.domain.BarberService;
import com.lacivita.turnos.catalog.domain.BarberServiceRepository;
import com.lacivita.turnos.catalog.domain.Combo;
import com.lacivita.turnos.catalog.domain.ComboRepository;
import com.lacivita.turnos.catalog.domain.Service;
import com.lacivita.turnos.catalog.domain.ServiceRepository;
import com.lacivita.turnos.catalog.domain.Terms;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Implementa la cotización que el catálogo ofrece a otros módulos. */
@Component
class ServiceQuotesService implements ServiceQuotes {

    private final ServiceRepository services;
    private final BarberServiceRepository offerings;
    private final ComboRepository combos;

    ServiceQuotesService(ServiceRepository services, BarberServiceRepository offerings, ComboRepository combos) {
        this.services = services;
        this.offerings = offerings;
        this.combos = combos;
    }

    @Override
    @BusinessScoped
    @Transactional(readOnly = true)
    public Optional<Quote> quote(@BusinessId UUID businessId, UUID barberId, BookableItem item) {
        var terms = switch (item) {
            case BookableItem.ServiceItem(UUID serviceId) -> serviceTerms(barberId, serviceId);
            case BookableItem.ComboItem(UUID comboId) -> comboTerms(barberId, comboId);
        };
        return terms.map(t -> new Quote(t.price(), t.duration().toDuration()));
    }

    private Optional<Terms> serviceTerms(UUID barberId, UUID serviceId) {
        return services.findById(serviceId)
                .filter(Service::isActive)
                .flatMap(service -> offerings
                        .findByBarberIdAndServiceId(barberId, serviceId)
                        .filter(BarberService::isActive)
                        .map(offering -> offering.termsFor(service)));
    }

    private Optional<Terms> comboTerms(UUID barberId, UUID comboId) {
        return combos.findById(comboId).filter(Combo::isActive).flatMap(combo -> {
            Map<UUID, Service> active = services.findAllByIdIn(combo.getServiceIds()).stream()
                    .filter(Service::isActive)
                    .collect(Collectors.toMap(Service::getId, Function.identity()));
            Map<UUID, Terms> barberTerms = offerings.findAllByBarberIdAndActiveTrue(barberId).stream()
                    .filter(offering -> active.containsKey(offering.getServiceId()))
                    .collect(Collectors.toMap(
                            BarberService::getServiceId,
                            offering -> offering.termsFor(active.get(offering.getServiceId()))));
            return combo.termsFrom(barberTerms);
        });
    }
}
