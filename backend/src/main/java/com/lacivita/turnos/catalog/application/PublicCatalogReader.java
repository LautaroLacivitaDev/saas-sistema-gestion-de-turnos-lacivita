package com.lacivita.turnos.catalog.application;

import com.lacivita.turnos.catalog.application.CatalogViews.BarberTermsView;
import com.lacivita.turnos.catalog.application.CatalogViews.PublicCatalogView;
import com.lacivita.turnos.catalog.application.CatalogViews.PublicComboView;
import com.lacivita.turnos.catalog.application.CatalogViews.PublicServiceView;
import com.lacivita.turnos.catalog.domain.BarberService;
import com.lacivita.turnos.catalog.domain.BarberServiceRepository;
import com.lacivita.turnos.catalog.domain.Combo;
import com.lacivita.turnos.catalog.domain.ComboRepository;
import com.lacivita.turnos.catalog.domain.Service;
import com.lacivita.turnos.catalog.domain.ServiceRepository;
import com.lacivita.turnos.catalog.domain.ServiceStatus;
import com.lacivita.turnos.catalog.domain.Terms;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import com.lacivita.turnos.users.TeamDirectory;
import com.lacivita.turnos.users.TeamMember;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Arma el catálogo de la página pública dentro del negocio (así lo protegen el filtro de Hibernate y Row
 * Level Security). Muestra solo lo que se puede reservar: servicios y combos activos que hace al menos
 * un profesional del equipo.
 */
@Component
class PublicCatalogReader {

    private static final Comparator<BarberTermsView> CHEAPEST_FIRST =
            Comparator.comparing(BarberTermsView::price).thenComparing(BarberTermsView::barberName);

    private final ServiceRepository services;
    private final BarberServiceRepository offerings;
    private final ComboRepository combos;
    private final TeamDirectory team;

    PublicCatalogReader(
            ServiceRepository services, BarberServiceRepository offerings, ComboRepository combos, TeamDirectory team) {
        this.services = services;
        this.offerings = offerings;
        this.combos = combos;
        this.team = team;
    }

    @BusinessScoped
    @Transactional(readOnly = true)
    public PublicCatalogView read(@BusinessId UUID businessId) {
        List<Service> active =
                services.findAllByBusinessIdAndStatusOrderByCategoryAscNameAsc(businessId, ServiceStatus.ACTIVE);
        Map<UUID, Service> servicesById =
                active.stream().collect(Collectors.toMap(Service::getId, Function.identity()));
        List<BarberService> current = offerings.findAllByServiceIdInAndActiveTrue(servicesById.keySet());
        Map<UUID, String> barberNames =
                team
                        .members(
                                businessId,
                                current.stream().map(BarberService::getBarberId).toList())
                        .stream()
                        .collect(Collectors.toMap(TeamMember::userId, TeamMember::name));

        // Condiciones de cada profesional del equipo en cada servicio: barbero → servicio → precio y duración.
        Map<UUID, Map<UUID, Terms>> termsByBarber = new HashMap<>();
        for (BarberService offering : current) {
            if (barberNames.containsKey(offering.getBarberId())) {
                termsByBarber
                        .computeIfAbsent(offering.getBarberId(), id -> new HashMap<>())
                        .put(offering.getServiceId(), offering.termsFor(servicesById.get(offering.getServiceId())));
            }
        }

        var serviceViews = active.stream()
                .map(service -> toView(service, termsByBarber, barberNames))
                .flatMap(Optional::stream)
                .toList();
        var comboViews = combos.findAllByBusinessIdAndActiveTrueOrderByNameAsc(businessId).stream()
                .map(combo -> toView(combo, termsByBarber, barberNames))
                .flatMap(Optional::stream)
                .toList();
        return new PublicCatalogView(serviceViews, comboViews);
    }

    private static Optional<PublicServiceView> toView(
            Service service, Map<UUID, Map<UUID, Terms>> termsByBarber, Map<UUID, String> barberNames) {
        var barbers = barbersWith(termsByBarber, barberNames, terms -> Optional.ofNullable(terms.get(service.getId())));
        if (barbers.isEmpty()) {
            return Optional.empty();
        }
        var details = service.details();
        return Optional.of(new PublicServiceView(
                service.getId(),
                details.name(),
                details.category(),
                details.description(),
                fromPrice(barbers),
                barbers));
    }

    private static Optional<PublicComboView> toView(
            Combo combo, Map<UUID, Map<UUID, Terms>> termsByBarber, Map<UUID, String> barberNames) {
        var barbers = barbersWith(termsByBarber, barberNames, combo::termsFrom);
        if (barbers.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new PublicComboView(
                combo.getId(), combo.getName(), combo.getServiceIds(), fromPrice(barbers), barbers));
    }

    /** Los profesionales que hacen el servicio o combo, del más barato al más caro. */
    private static List<BarberTermsView> barbersWith(
            Map<UUID, Map<UUID, Terms>> termsByBarber,
            Map<UUID, String> barberNames,
            Function<Map<UUID, Terms>, Optional<Terms>> termsOf) {
        return termsByBarber.entrySet().stream()
                .flatMap(entry -> termsOf.apply(entry.getValue()).stream()
                        .map(terms -> new BarberTermsView(
                                entry.getKey(),
                                barberNames.get(entry.getKey()),
                                terms.price().amount(),
                                terms.duration().minutes())))
                .sorted(CHEAPEST_FIRST)
                .toList();
    }

    private static BigDecimal fromPrice(List<BarberTermsView> cheapestFirst) {
        return cheapestFirst.getFirst().price();
    }
}
