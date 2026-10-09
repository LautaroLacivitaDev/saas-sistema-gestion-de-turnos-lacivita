package com.lacivita.turnos.catalog.application;

import com.lacivita.turnos.catalog.application.CatalogViews.ComboView;
import com.lacivita.turnos.catalog.application.CatalogViews.OfferingView;
import com.lacivita.turnos.catalog.application.CatalogViews.ServiceView;
import com.lacivita.turnos.catalog.domain.BarberService;
import com.lacivita.turnos.catalog.domain.Combo;
import com.lacivita.turnos.catalog.domain.PriceRange;
import com.lacivita.turnos.catalog.domain.Service;
import com.lacivita.turnos.catalog.domain.ServiceDetails;
import com.lacivita.turnos.catalog.domain.ServiceDuration;
import com.lacivita.turnos.shared.domain.Money;
import java.math.BigDecimal;
import java.util.UUID;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper
interface CatalogMapper {

    @Mapping(target = "id", source = "id")
    @Mapping(target = "name", source = "details.name")
    @Mapping(target = "category", source = "details.category")
    @Mapping(target = "description", source = "details.description")
    @Mapping(target = "baseDurationMinutes", source = "details.baseDuration.minutes")
    @Mapping(target = "basePrice", source = "details.basePrice.amount")
    @Mapping(target = "priceMin", source = "range.min.amount")
    @Mapping(target = "priceMax", source = "range.max.amount")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "proposedBy", source = "proposedBy")
    ServiceView toView(UUID id, ServiceDetails details, PriceRange range, String status, UUID proposedBy);

    default ServiceView toView(Service service) {
        return toView(
                service.getId(),
                service.details(),
                service.priceRange().orElse(null),
                service.getStatus().name(),
                service.proposedBy().orElse(null));
    }

    ComboView toView(Combo combo);

    default OfferingView toView(BarberService offering, Service service) {
        var terms = offering.termsFor(service);
        return new OfferingView(
                offering.getId(),
                offering.getBarberId(),
                service.getId(),
                service.getName(),
                amount(offering.ownPrice().orElse(null)),
                offering.ownDuration().map(ServiceDuration::minutes).orElse(null),
                terms.price().amount(),
                terms.duration().minutes(),
                amount(offering.requestedPrice().orElse(null)));
    }

    default BigDecimal amount(Money money) {
        return money == null ? null : money.amount();
    }
}
