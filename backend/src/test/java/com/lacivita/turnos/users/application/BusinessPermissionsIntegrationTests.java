package com.lacivita.turnos.users.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lacivita.turnos.IntegrationTest;
import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.security.BusinessRole;
import com.lacivita.turnos.shared.security.PlatformRole;
import com.lacivita.turnos.users.domain.Membership;
import com.lacivita.turnos.users.domain.MembershipRepository;
import com.lacivita.turnos.users.domain.NewPassword;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Permisos por negocio de punta a punta: membresías reales en PostgreSQL, {@code @PreAuthorize} y el
 * evaluador de permisos.
 */
@IntegrationTest
@Import(BusinessPermissionsIntegrationTests.ProbeConfiguration.class)
class BusinessPermissionsIntegrationTests {

    @Autowired
    BusinessProbe probe;

    @Autowired
    AccountRegistration registration;

    @Autowired
    MembershipRepository memberships;

    @Autowired
    TransactionTemplate transaction;

    final UUID business = UUID.randomUUID();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @EnumSource(BusinessRole.class)
    void everyRoleCanDoWhatABarberCanDo(BusinessRole role) {
        signInAs(memberWithRole(role));

        assertThat(probe.barberAction(business)).isEqualTo("ok");
    }

    @Test
    void onlyManagersAndOwnersCanDoManagerActions() {
        signInAs(memberWithRole(BusinessRole.BARBER));
        assertThatThrownBy(() -> probe.managerAction(business)).isInstanceOf(AccessDeniedException.class);

        signInAs(memberWithRole(BusinessRole.MANAGER));
        assertThat(probe.managerAction(business)).isEqualTo("ok");

        signInAs(memberWithRole(BusinessRole.OWNER));
        assertThat(probe.managerAction(business)).isEqualTo("ok");
    }

    @Test
    void onlyTheOwnerCanDoOwnerActions() {
        signInAs(memberWithRole(BusinessRole.MANAGER));
        assertThatThrownBy(() -> probe.ownerAction(business)).isInstanceOf(AccessDeniedException.class);

        signInAs(memberWithRole(BusinessRole.OWNER));
        assertThat(probe.ownerAction(business)).isEqualTo("ok");
    }

    @Test
    void customersWithoutMembershipCannotActOnTheBusiness() {
        signInAs(newUser(PlatformRole.USER));

        assertThatThrownBy(() -> probe.barberAction(business)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void anOwnerOfAnotherBusinessCannotActOnThisOne() {
        var ownerElsewhere = newUser(PlatformRole.USER);
        grant(ownerElsewhere, UUID.randomUUID(), BusinessRole.OWNER);
        signInAs(ownerElsewhere);

        assertThatThrownBy(() -> probe.barberAction(business)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void platformAdminsCanActOnAnyBusiness() {
        signInAs(newUser(PlatformRole.ADMIN));

        assertThat(probe.ownerAction(business)).isEqualTo("ok");
    }

    @Test
    void platformAdminsInheritUserPermissions() {
        signInAs(newUser(PlatformRole.ADMIN));

        assertThat(probe.userAction()).isEqualTo("ok");
    }

    private AuthenticatedUser memberWithRole(BusinessRole role) {
        var user = newUser(PlatformRole.USER);
        grant(user, business, role);
        return user;
    }

    private AuthenticatedUser newUser(PlatformRole role) {
        var email = "permisos-" + UUID.randomUUID() + "@example.com";
        var account = registration.register("Persona", new Email(email), new NewPassword("clave-segura-1"));
        var principal = account.principal();
        return new AuthenticatedUser(principal.id(), principal.email(), principal.name(), role);
    }

    private void grant(AuthenticatedUser user, UUID businessId, BusinessRole role) {
        transaction.executeWithoutResult(
                status -> memberships.save(Membership.grant(user.id(), businessId, role, Instant.now())));
    }

    private static void signInAs(AuthenticatedUser user) {
        var authority = new SimpleGrantedAuthority("ROLE_" + user.platformRole().name());
        SecurityContextHolder.getContext()
                .setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, List.of(authority)));
    }

    /** Servicio de prueba con las tres exigencias de rol, como las usarán los módulos de negocio. */
    static class BusinessProbe {

        @PreAuthorize("hasPermission(#businessId, 'Business', 'BARBER')")
        public String barberAction(UUID businessId) {
            return "ok";
        }

        @PreAuthorize("hasPermission(#businessId, 'Business', 'MANAGER')")
        public String managerAction(UUID businessId) {
            return "ok";
        }

        @PreAuthorize("hasPermission(#businessId, 'Business', 'OWNER')")
        public String ownerAction(UUID businessId) {
            return "ok";
        }

        @PreAuthorize("hasRole('USER')")
        public String userAction() {
            return "ok";
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class ProbeConfiguration {

        @Bean
        BusinessProbe businessProbe() {
            return new BusinessProbe();
        }
    }
}
