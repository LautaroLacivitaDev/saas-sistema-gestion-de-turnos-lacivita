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
import java.util.ArrayList;
import java.util.List;
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
        return switch (item) {
            case BookableItem.ServiceItem(UUID serviceId) -> serviceQuote(barberId, serviceId);
            case BookableItem.ComboItem(UUID comboId) -> comboQuote(barberId, comboId);
        };
    }

    private Optional<Quote> serviceQuote(UUID barberId, UUID serviceId) {
        return services.findById(serviceId)
                .filter(Service::isActive)
                .flatMap(service -> offerings
                        .findByBarberIdAndServiceId(barberId, serviceId)
                        .filter(BarberService::isActive)
                        .map(offering -> quoteOf(List.of(service), Map.of(serviceId, offering.termsFor(service)))));
    }

    private Optional<Quote> comboQuote(UUID barberId, UUID comboId) {
        return combos.findById(comboId).filter(Combo::isActive).flatMap(combo -> {
            Map<UUID, Service> active = services.findAllByIdIn(combo.getServiceIds()).stream()
                    .filter(Service::isActive)
                    .collect(Collectors.toMap(Service::getId, Function.identity()));
            Map<UUID, Terms> barberTerms = offerings.findAllByBarberIdAndActiveTrue(barberId).stream()
                    .filter(offering -> active.containsKey(offering.getServiceId()))
                    .collect(Collectors.toMap(
                            BarberService::getServiceId,
                            offering -> offering.termsFor(active.get(offering.getServiceId()))));
            if (combo.termsFrom(barberTerms).isEmpty()) {
                // El profesional no hace alguno de los servicios del combo (o alguno está retirado).
                return Optional.empty();
            }
            return Optional.of(
                    quoteOf(combo.getServiceIds().stream().map(active::get).toList(), barberTerms));
        });
    }

    /** Precio y duración totales y el detalle de cada servicio, en orden. */
    private static Quote quoteOf(List<Service> inOrder, Map<UUID, Terms> termsByService) {
        var lines = new ArrayList<Quote.Line>();
        Terms total = null;
        for (Service service : inOrder) {
            var terms = termsByService.get(service.getId());
            lines.add(new Quote.Line(
                    service.getId(),
                    service.getName(),
                    terms.price(),
                    terms.duration().toDuration()));
            total = total == null ? terms : total.plus(terms);
        }
        return new Quote(total.price(), total.duration().toDuration(), lines);
    }
}
