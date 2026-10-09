package com.lacivita.turnos.catalog.domain;

import static com.lacivita.turnos.catalog.domain.CatalogFixtures.BUSINESS;
import static com.lacivita.turnos.catalog.domain.CatalogFixtures.NOW;
import static com.lacivita.turnos.catalog.domain.CatalogFixtures.activeService;
import static com.lacivita.turnos.catalog.domain.CatalogFixtures.details;
import static com.lacivita.turnos.catalog.domain.CatalogFixtures.serviceWithRange;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lacivita.turnos.shared.domain.InvalidValueException;
import com.lacivita.turnos.shared.domain.Money;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ServiceTests {

    @Test
    void aServiceCreatedByAManagerIsActiveRightAway() {
        var service = activeService("Corte", 30, "8000");

        assertThat(service.isActive()).isTrue();
        assertThat(service.proposedBy()).isEmpty();
        assertThat(service.baseTerms()).isEqualTo(new Terms(Money.of("8000"), new ServiceDuration(30)));
    }

    @Test
    void aProposalWaitsForApprovalAndCanBeApprovedOnce() {
        var barber = UUID.randomUUID();
        var service = Service.propose(BUSINESS, details("Diseño de cejas", 15, "3000"), barber, NOW);

        assertThat(service.isActive()).isFalse();
        assertThat(service.proposedBy()).contains(barber);

        service.approve(NOW);

        assertThat(service.isActive()).isTrue();
        assertThatThrownBy(() -> service.approve(NOW)).isInstanceOf(ServiceStatusException.class);
    }

    @Test
    void aRejectedProposalCanNoLongerBeEditedOrApproved() {
        var service = Service.propose(BUSINESS, details("Diseño de cejas", 15, "3000"), UUID.randomUUID(), NOW);
        service.reject(NOW);

        assertThatThrownBy(() -> service.update(details("Cejas", 15, "3000"), NOW))
                .isInstanceOf(ServiceStatusException.class);
        assertThatThrownBy(() -> service.approve(NOW)).isInstanceOf(ServiceStatusException.class);
    }

    @Test
    void aWithdrawnServiceCanComeBack() {
        var service = activeService("Corte", 30, "8000");

        service.deactivate(NOW);
        assertThat(service.isActive()).isFalse();
        assertThatThrownBy(() -> service.deactivate(NOW)).isInstanceOf(ServiceStatusException.class);

        service.reactivate(NOW);
        assertThat(service.isActive()).isTrue();
    }

    @Test
    void theBasePriceHasToBeWithinThePriceRange() {
        var service = serviceWithRange("8000", "7000", "10000");

        assertThatThrownBy(() -> service.update(details("Corte", 30, "12000"), NOW))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> service.limitPrices(new PriceRange(Money.of("9000"), Money.of("10000")), NOW))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void theRangeCanBeRemoved() {
        var service = serviceWithRange("8000", "7000", "10000");

        service.limitPrices(null, NOW);

        assertThat(service.priceRange()).isEmpty();
    }

    @Test
    void aSingleServiceLastsAtMostEightHours() {
        assertThatThrownBy(() -> details("Maratón", 485, "1000")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> details("Corte", 32, "1000")).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void aRangeNeedsBothEndsInOrder() {
        assertThatThrownBy(() -> new PriceRange(Money.of("10"), null)).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new PriceRange(Money.of("10"), Money.of("5")))
                .isInstanceOf(InvalidValueException.class);
    }
}
