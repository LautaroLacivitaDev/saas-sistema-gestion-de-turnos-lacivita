package com.lacivita.turnos.catalog.domain;

import static com.lacivita.turnos.catalog.domain.CatalogFixtures.BUSINESS;
import static com.lacivita.turnos.catalog.domain.CatalogFixtures.NOW;
import static com.lacivita.turnos.catalog.domain.CatalogFixtures.activeService;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lacivita.turnos.shared.domain.InvalidValueException;
import com.lacivita.turnos.shared.domain.Money;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ComboTests {

    final Service corte = activeService("Corte", 30, "8000");
    final Service barba = activeService("Barba", 20, "5000");

    @Test
    void priceAndDurationAreTheSumOfTheBarberTermsInEachService() {
        var combo = Combo.compose(BUSINESS, "Corte y barba", List.of(corte, barba), NOW);

        var terms = combo.termsFrom(Map.of(
                corte.getId(), new Terms(Money.of("9000"), new ServiceDuration(30)),
                barba.getId(), new Terms(Money.of("5000"), new ServiceDuration(25))));

        assertThat(terms).contains(new Terms(Money.of("14000"), new ServiceDuration(55)));
    }

    @Test
    void aBarberWhoDoesNotDoEveryServiceCannotDoTheCombo() {
        var combo = Combo.compose(BUSINESS, "Corte y barba", List.of(corte, barba), NOW);

        assertThat(combo.termsFrom(Map.of(corte.getId(), corte.baseTerms()))).isEmpty();
    }

    @Test
    void aComboKeepsTheOrderOfItsServices() {
        var combo = Combo.compose(BUSINESS, "Barba y corte", List.of(barba, corte), NOW);

        assertThat(combo.getServiceIds()).containsExactly(barba.getId(), corte.getId());
    }

    @Test
    void aComboHasBetweenTwoAndFiveDifferentServices() {
        assertThatThrownBy(() -> Combo.compose(BUSINESS, "Solo corte", List.of(corte), NOW))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> Combo.compose(BUSINESS, "Doble corte", List.of(corte, corte), NOW))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void onlyActiveServicesOfTheSameBusinessCanBeCombined() {
        var retired = activeService("Brushing", 30, "6000");
        retired.deactivate(NOW);
        var foreign = Service.create(UUID.randomUUID(), CatalogFixtures.details("Ajeno", 30, "1000"), NOW);

        assertThatThrownBy(() -> Combo.compose(BUSINESS, "Con retirado", List.of(corte, retired), NOW))
                .isInstanceOf(ServiceStatusException.class);
        assertThatThrownBy(() -> Combo.compose(BUSINESS, "Con ajeno", List.of(corte, foreign), NOW))
                .isInstanceOf(ServiceStatusException.class);
    }
}
