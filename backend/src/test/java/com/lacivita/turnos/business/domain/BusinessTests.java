package com.lacivita.turnos.business.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lacivita.turnos.shared.domain.InvalidValueException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BusinessTests {

    static final Instant NOW = Instant.parse("2026-10-08T12:00:00Z");
    static final BusinessProfile PROFILE =
            new BusinessProfile("Barbería Don Pepe", BusinessCategory.BARBERSHOP, "Cortes clásicos", true);

    @Test
    void aNewBusinessUsesItsInitialSlug() {
        var business = Business.register(UUID.randomUUID(), PROFILE, new Slug("don-pepe"), NOW);

        assertThat(business.getSlug()).isEqualTo("don-pepe");
        assertThat(business.hasUsed(new Slug("don-pepe"))).isTrue();
    }

    @Test
    void changingTheSlugKeepsTheOldOneForRedirects() {
        var business = Business.register(UUID.randomUUID(), PROFILE, new Slug("don-pepe"), NOW);

        business.changeSlug(new Slug("don-pepe-centro"), NOW);

        assertThat(business.getSlug()).isEqualTo("don-pepe-centro");
        assertThat(business.hasUsed(new Slug("don-pepe"))).isTrue();
        assertThat(business.hasUsed(new Slug("don-pepe-centro"))).isTrue();
    }

    @Test
    void aBusinessCanGoBackToASlugItUsedBefore() {
        var business = Business.register(UUID.randomUUID(), PROFILE, new Slug("don-pepe"), NOW);
        business.changeSlug(new Slug("don-pepe-centro"), NOW);

        business.changeSlug(new Slug("don-pepe"), NOW);

        assertThat(business.getSlug()).isEqualTo("don-pepe");
    }

    @Test
    void updatingTheProfileReplacesTheEditableData() {
        var business = Business.register(UUID.randomUUID(), PROFILE, new Slug("don-pepe"), NOW);
        var updated = new BusinessProfile("Don Pepe Estética", BusinessCategory.BEAUTY_SALON, null, false);

        business.updateProfile(updated, NOW);

        assertThat(business.profile()).isEqualTo(updated);
    }

    @Test
    void theNameIsRequiredAndBlankDescriptionsAreDropped() {
        assertThatThrownBy(() -> new BusinessProfile(" ", BusinessCategory.BARBERSHOP, null, true))
                .isInstanceOf(InvalidValueException.class);
        assertThat(new BusinessProfile("X", BusinessCategory.BARBERSHOP, "   ", true).description())
                .isNull();
    }
}
