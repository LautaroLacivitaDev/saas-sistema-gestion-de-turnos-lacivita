package com.lacivita.turnos.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lacivita.turnos.shared.domain.Email;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class BusinessPermissionEvaluatorTests {

    static final UUID BUSINESS = UUID.randomUUID();
    static final UUID OTHER_BUSINESS = UUID.randomUUID();
    static final UUID CENTRO = UUID.randomUUID();
    static final UUID NORTE = UUID.randomUUID();

    final Map<UUID, BusinessMembership> membershipsInBusiness = new HashMap<>();
    boolean supportGranted;

    final BusinessPermissionEvaluator evaluator = new BusinessPermissionEvaluator(
            (userId, businessId) -> businessId.equals(BUSINESS)
                    ? Optional.ofNullable(membershipsInBusiness.get(userId))
                    : Optional.empty(),
            branchId -> branchId.equals(CENTRO) || branchId.equals(NORTE) ? Optional.of(BUSINESS) : Optional.empty(),
            (admin, businessId) -> admin.isAdmin() && supportGranted);

    @ParameterizedTest(name = "{0} pide {1}: {2}")
    @CsvSource({
        "OWNER,   OWNER,   true",
        "OWNER,   MANAGER, true",
        "OWNER,   BARBER,  true",
        "MANAGER, OWNER,   false",
        "MANAGER, MANAGER, true",
        "MANAGER, BARBER,  true",
        "BARBER,  OWNER,   false",
        "BARBER,  MANAGER, false",
        "BARBER,  BARBER,  true",
    })
    void eachRoleIncludesTheOnesBelowIt(BusinessRole role, BusinessRole required, boolean expected) {
        var user = member(role, Set.of(CENTRO));

        assertThat(evaluator.hasPermission(authenticated(user), BUSINESS, "Business", required.name()))
                .isEqualTo(expected);
    }

    @Test
    void aRoleInOneBusinessGrantsNothingInAnother() {
        var owner = member(BusinessRole.OWNER, Set.of());

        assertThat(evaluator.hasPermission(authenticated(owner), OTHER_BUSINESS, "Business", "BARBER"))
                .isFalse();
    }

    @Test
    void staffCanOnlyActOnTheirAssignedBranches() {
        var barber = member(BusinessRole.BARBER, Set.of(CENTRO));

        assertThat(evaluator.hasPermission(authenticated(barber), CENTRO, "Branch", "BARBER"))
                .isTrue();
        assertThat(evaluator.hasPermission(authenticated(barber), NORTE, "Branch", "BARBER"))
                .isFalse();
    }

    @Test
    void theOwnerActsOnEveryBranch() {
        var owner = member(BusinessRole.OWNER, Set.of());

        assertThat(evaluator.hasPermission(authenticated(owner), NORTE, "Branch", "OWNER"))
                .isTrue();
    }

    @Test
    void unknownBranchesAreDenied() {
        var owner = member(BusinessRole.OWNER, Set.of());

        assertThat(evaluator.hasPermission(authenticated(owner), UUID.randomUUID(), "Branch", "BARBER"))
                .isFalse();
    }

    @Test
    void customersWithoutMembershipHaveNoBusinessPermissions() {
        var customer = user(PlatformRole.USER);

        assertThat(evaluator.hasPermission(authenticated(customer), BUSINESS, "Business", "BARBER"))
                .isFalse();
    }

    @Test
    void adminsGetInOnlyThroughJustifiedSupportAccess() {
        var admin = user(PlatformRole.ADMIN);

        supportGranted = false;
        assertThat(evaluator.hasPermission(authenticated(admin), BUSINESS, "Business", "OWNER"))
                .isFalse();

        supportGranted = true;
        assertThat(evaluator.hasPermission(authenticated(admin), BUSINESS, "Business", "OWNER"))
                .isTrue();
    }

    @Test
    void anonymousOrForeignPrincipalsAndChecksWithoutIdAreDenied() {
        assertThat(evaluator.hasPermission(null, BUSINESS, "Business", "BARBER"))
                .isFalse();
        assertThat(evaluator.hasPermission(
                        new TestingAuthenticationToken("alguien", null), BUSINESS, "Business", "BARBER"))
                .isFalse();
        assertThat(evaluator.hasPermission(authenticated(user(PlatformRole.ADMIN)), new Object(), "OWNER"))
                .isFalse();
    }

    @Test
    void unknownResourceTypesAreAProgrammingError() {
        var owner = member(BusinessRole.OWNER, Set.of());

        assertThatThrownBy(() -> evaluator.hasPermission(authenticated(owner), BUSINESS, "Appointment", "OWNER"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private AuthenticatedUser member(BusinessRole role, Set<UUID> branches) {
        var user = user(PlatformRole.USER);
        membershipsInBusiness.put(user.id(), new BusinessMembership(role, branches));
        return user;
    }

    private static AuthenticatedUser user(PlatformRole role) {
        return new AuthenticatedUser(UUID.randomUUID(), new Email("persona@example.com"), "Persona", role);
    }

    private static Authentication authenticated(AuthenticatedUser user) {
        return UsernamePasswordAuthenticationToken.authenticated(user, null, user.authorities());
    }
}
