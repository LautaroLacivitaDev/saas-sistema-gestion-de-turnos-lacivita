package com.lacivita.turnos.catalog.domain;

import static com.lacivita.turnos.catalog.domain.CatalogFixtures.BUSINESS;
import static com.lacivita.turnos.catalog.domain.CatalogFixtures.NOW;
import static com.lacivita.turnos.catalog.domain.CatalogFixtures.activeService;
import static com.lacivita.turnos.catalog.domain.CatalogFixtures.details;
import static com.lacivita.turnos.catalog.domain.CatalogFixtures.serviceWithRange;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lacivita.turnos.catalog.domain.BarberService.PriceOutcome;
import com.lacivita.turnos.shared.domain.Money;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BarberServiceTests {

    static final UUID BARBER = UUID.randomUUID();

    @Test
    void withoutOwnValuesTheBarberInheritsTheBaseOnes() {
        var service = activeService("Corte", 30, "8000");

        var offering = BarberService.offer(BARBER, service, NOW);

        assertThat(offering.termsFor(service)).isEqualTo(service.baseTerms());
    }

    @Test
    void ownPriceAndDurationReplaceTheBaseOnes() {
        var service = activeService("Corte", 30, "8000");
        var offering = BarberService.offer(BARBER, service, NOW);

        var outcome = offering.changeTerms(service, Money.of("9500"), new ServiceDuration(45), false, NOW);

        assertThat(outcome).isEqualTo(PriceOutcome.APPLIED);
        assertThat(offering.termsFor(service)).isEqualTo(new Terms(Money.of("9500"), new ServiceDuration(45)));
    }

    @Test
    void aBarberPriceWithinTheRangeAppliesRightAway() {
        var service = serviceWithRange("8000", "7000", "10000");
        var offering = BarberService.offer(BARBER, service, NOW);

        assertThat(offering.changeTerms(service, Money.of("10000"), null, false, NOW))
                .isEqualTo(PriceOutcome.APPLIED);
        assertThat(offering.termsFor(service).price()).isEqualTo(Money.of("10000"));
    }

    @Test
    void aBarberPriceOutsideTheRangeWaitsAndTheCurrentPriceKeepsApplying() {
        var service = serviceWithRange("8000", "7000", "10000");
        var offering = BarberService.offer(BARBER, service, NOW);

        var outcome = offering.changeTerms(service, Money.of("12000"), null, false, NOW);

        assertThat(outcome).isEqualTo(PriceOutcome.AWAITING_APPROVAL);
        assertThat(offering.termsFor(service).price()).isEqualTo(Money.of("8000"));
        assertThat(offering.requestedPrice()).contains(Money.of("12000"));
    }

    @Test
    void approvingAppliesTheRequestedPrice() {
        var service = serviceWithRange("8000", "7000", "10000");
        var offering = BarberService.offer(BARBER, service, NOW);
        offering.changeTerms(service, Money.of("12000"), null, false, NOW);

        offering.approveRequestedPrice(NOW);

        assertThat(offering.termsFor(service).price()).isEqualTo(Money.of("12000"));
        assertThat(offering.requestedPrice()).isEmpty();
    }

    @Test
    void rejectingKeepsThePreviousPrice() {
        var service = serviceWithRange("8000", "7000", "10000");
        var offering = BarberService.offer(BARBER, service, NOW);
        offering.changeTerms(service, Money.of("12000"), null, false, NOW);

        offering.rejectRequestedPrice(NOW);

        assertThat(offering.termsFor(service).price()).isEqualTo(Money.of("8000"));
        assertThatThrownBy(() -> offering.rejectRequestedPrice(NOW)).isInstanceOf(NoPendingPriceException.class);
    }

    @Test
    void managersAndOwnersApplyPricesOutsideTheRangeDirectly() {
        var service = serviceWithRange("8000", "7000", "10000");
        var offering = BarberService.offer(BARBER, service, NOW);

        assertThat(offering.changeTerms(service, Money.of("15000"), null, true, NOW))
                .isEqualTo(PriceOutcome.APPLIED);
        assertThat(offering.termsFor(service).price()).isEqualTo(Money.of("15000"));
    }

    @Test
    void goingBackToTheBasePriceNeedsNoApprovalAndCancelsAPendingRequest() {
        var service = serviceWithRange("8000", "7000", "10000");
        var offering = BarberService.offer(BARBER, service, NOW);
        offering.changeTerms(service, Money.of("12000"), null, false, NOW);

        assertThat(offering.changeTerms(service, null, null, false, NOW)).isEqualTo(PriceOutcome.APPLIED);
        assertThat(offering.requestedPrice()).isEmpty();
        assertThat(offering.termsFor(service)).isEqualTo(service.baseTerms());
    }

    @Test
    void onlyActiveServicesCanBeOffered() {
        var proposal = Service.propose(BUSINESS, details("Cejas", 15, "3000"), UUID.randomUUID(), NOW);

        assertThatThrownBy(() -> BarberService.offer(BARBER, proposal, NOW)).isInstanceOf(ServiceStatusException.class);
    }

    @Test
    void resumingKeepsTheOwnValues() {
        var service = activeService("Corte", 30, "8000");
        var offering = BarberService.offer(BARBER, service, NOW);
        offering.changeTerms(service, Money.of("9000"), null, false, NOW);

        offering.withdraw(NOW);
        offering.resume(service, NOW);

        assertThat(offering.isActive()).isTrue();
        assertThat(offering.termsFor(service).price()).isEqualTo(Money.of("9000"));
    }
}
