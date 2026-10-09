package com.lacivita.turnos.catalog.domain;

import com.lacivita.turnos.shared.domain.Money;
import java.time.Instant;
import java.util.UUID;

/** Datos de prueba del catálogo. */
final class CatalogFixtures {

    static final Instant NOW = Instant.parse("2026-10-08T12:00:00Z");
    static final UUID BUSINESS = UUID.randomUUID();

    private CatalogFixtures() {}

    static ServiceDetails details(String name, int minutes, String price) {
        return new ServiceDetails(name, "Cortes", null, new ServiceDuration(minutes), Money.of(price));
    }

    static Service activeService(String name, int minutes, String price) {
        return Service.create(BUSINESS, details(name, minutes, price), NOW);
    }

    static Service serviceWithRange(String price, String min, String max) {
        var service = activeService("Corte", 30, price);
        service.limitPrices(new PriceRange(Money.of(min), Money.of(max)), NOW);
        return service;
    }
}
