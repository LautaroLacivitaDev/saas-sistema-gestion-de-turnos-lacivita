package com.lacivita.turnos.shared.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lacivita.turnos.shared.domain.Email;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
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

    final Map<UUID, BusinessRole> rolesInBusiness = new HashMap<>();
    final BusinessPermissionEvaluator evaluator = new BusinessPermissionEvaluator((userId, businessId) ->
            businessId.equals(BUSINESS) ? Optional.ofNullable(rolesInBusiness.get(userId)) : Optional.empty());

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
        var user = user(PlatformRole.USER);
        rolesInBusiness.put(user.id(), role);

        assertThat(evaluator.hasPermission(authenticated(user), BUSINESS, "Business", required.name()))
                .isEqualTo(expected);
    }

    @Test
    void aRoleInOneBusinessGrantsNothingInAnother() {
        var user = user(PlatformRole.USER);
        rolesInBusiness.put(user.id(), BusinessRole.OWNER);

        assertThat(evaluator.hasPermission(authenticated(user), OTHER_BUSINESS, "Business", "BARBER"))
                .isFalse();
    }

    @Test
    void customersWithoutMembershipHaveNoBusinessPermissions() {
        var customer = user(PlatformRole.USER);

        assertThat(evaluator.hasPermission(authenticated(customer), BUSINESS, "Business", "BARBER"))
                .isFalse();
    }

    @Test
    void platformAdminsCanActOnAnyBusiness() {
        var admin = user(PlatformRole.ADMIN);

        assertThat(evaluator.hasPermission(authenticated(admin), OTHER_BUSINESS, "Business", "OWNER"))
                .isTrue();
    }

    @Test
    void anonymousOrForeignPrincipalsAreDenied() {
        assertThat(evaluator.hasPermission(null, BUSINESS, "Business", "BARBER"))
                .isFalse();
        assertThat(evaluator.hasPermission(
                        new TestingAuthenticationToken("alguien", null), BUSINESS, "Business", "BARBER"))
                .isFalse();
    }

    @Test
    void checksWithoutAResourceIdAreAlwaysDenied() {
        var user = user(PlatformRole.ADMIN);

        assertThat(evaluator.hasPermission(authenticated(user), new Object(), "OWNER"))
                .isFalse();
    }

    @Test
    void unknownResourceTypesAreAProgrammingError() {
        var user = user(PlatformRole.USER);

        assertThatThrownBy(() -> evaluator.hasPermission(authenticated(user), BUSINESS, "Branch", "OWNER"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static AuthenticatedUser user(PlatformRole role) {
        return new AuthenticatedUser(UUID.randomUUID(), new Email("persona@example.com"), "Persona", role);
    }

    private static Authentication authenticated(AuthenticatedUser user) {
        return UsernamePasswordAuthenticationToken.authenticated(user, null, user.authorities());
    }
}
