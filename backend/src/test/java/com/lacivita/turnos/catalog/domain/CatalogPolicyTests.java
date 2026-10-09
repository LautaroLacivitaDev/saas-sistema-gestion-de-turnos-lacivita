package com.lacivita.turnos.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lacivita.turnos.shared.security.BusinessMembership;
import com.lacivita.turnos.shared.security.BusinessRole;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CatalogPolicyTests {

    static final UUID CENTRO = UUID.randomUUID();
    static final UUID NORTE = UUID.randomUUID();
    static final UUID ACTOR = UUID.randomUUID();
    static final UUID OTHER = UUID.randomUUID();

    static final BusinessMembership OWNER = new BusinessMembership(BusinessRole.OWNER, Set.of());
    static final BusinessMembership MANAGER_CENTRO = new BusinessMembership(BusinessRole.MANAGER, Set.of(CENTRO));
    static final BusinessMembership BARBER_CENTRO = new BusinessMembership(BusinessRole.BARBER, Set.of(CENTRO));
    static final BusinessMembership BARBER_NORTE = new BusinessMembership(BusinessRole.BARBER, Set.of(NORTE));

    @Test
    void everyoneManagesTheirOwnOfferings() {
        assertThatCode(() -> CatalogPolicy.checkCanManageOfferingsOf(ACTOR, BARBER_CENTRO, ACTOR, BARBER_CENTRO))
                .doesNotThrowAnyException();
    }

    @Test
    void theOwnerManagesEveryonesOfferings() {
        assertThatCode(() -> CatalogPolicy.checkCanManageOfferingsOf(ACTOR, OWNER, OTHER, MANAGER_CENTRO))
                .doesNotThrowAnyException();
    }

    @Test
    void aManagerManagesOnlyBarbersOfTheirBranches() {
        assertThatCode(() -> CatalogPolicy.checkCanManageOfferingsOf(ACTOR, MANAGER_CENTRO, OTHER, BARBER_CENTRO))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> CatalogPolicy.checkCanManageOfferingsOf(ACTOR, MANAGER_CENTRO, OTHER, BARBER_NORTE))
                .isInstanceOf(CatalogActionNotAllowedException.class);
        assertThatThrownBy(() -> CatalogPolicy.checkCanManageOfferingsOf(ACTOR, MANAGER_CENTRO, OTHER, MANAGER_CENTRO))
                .isInstanceOf(CatalogActionNotAllowedException.class);
    }

    @Test
    void aBarberCannotTouchSomeoneElsesOfferings() {
        assertThatThrownBy(() -> CatalogPolicy.checkCanManageOfferingsOf(ACTOR, BARBER_CENTRO, OTHER, BARBER_CENTRO))
                .isInstanceOf(CatalogActionNotAllowedException.class);
    }

    @Test
    void managersAndOwnersApprovePricesBarbersDoNot() {
        assertThat(CatalogPolicy.approvesPrices(OWNER)).isTrue();
        assertThat(CatalogPolicy.approvesPrices(MANAGER_CENTRO)).isTrue();
        assertThat(CatalogPolicy.approvesPrices(BARBER_CENTRO)).isFalse();
    }
}
