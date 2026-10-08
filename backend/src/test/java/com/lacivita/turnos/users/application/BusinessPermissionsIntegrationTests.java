package com.lacivita.turnos.users.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lacivita.turnos.ApiClient;
import com.lacivita.turnos.IntegrationTest;
import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.security.BusinessRole;
import com.lacivita.turnos.shared.security.PlatformRole;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import com.lacivita.turnos.shared.tenancy.TenantContext;
import com.lacivita.turnos.users.domain.MembershipFixtures;
import com.lacivita.turnos.users.domain.MembershipRepository;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Permisos de punta a punta: membresías reales en PostgreSQL, {@code @PreAuthorize}, el contexto del
 * negocio y el evaluador de permisos, por negocio y por sucursal.
 */
@IntegrationTest
@Import(BusinessPermissionsIntegrationTests.ProbeConfiguration.class)
class BusinessPermissionsIntegrationTests {

    @Autowired
    BusinessProbe probe;

    @Autowired
    MockMvcTester mvc;

    @Autowired
    MembershipRepository memberships;

    @Autowired
    TransactionTemplate transaction;

    ApiClient api;
    UUID business;
    UUID centro;
    UUID norte;

    @BeforeEach
    void businessWithTwoBranches() {
        api = new ApiClient(mvc);
        var owner = api.registerNewUser("Dueña");
        business = api.createBusiness(
                owner, "permisos-" + UUID.randomUUID().toString().substring(0, 8));
        centro = api.createBranch(owner, business, "Centro");
        norte = api.createBranch(owner, business, "Norte");
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @EnumSource(
            value = BusinessRole.class,
            names = {"MANAGER", "BARBER"})
    void staffCanDoBarberActionsInTheirBusiness(BusinessRole role) {
        signInAs(member(role, centro));

        assertThat(probe.barberAction(business)).isEqualTo("ok");
    }

    @Test
    void onlyManagersAndOwnersCanDoManagerActions() {
        signInAs(member(BusinessRole.BARBER, centro));
        assertThatThrownBy(() -> probe.managerAction(business)).isInstanceOf(AccessDeniedException.class);

        signInAs(member(BusinessRole.MANAGER, centro));
        assertThat(probe.managerAction(business)).isEqualTo("ok");
    }

    @Test
    void staffActsOnlyOnTheirAssignedBranches() {
        signInAs(member(BusinessRole.MANAGER, centro));

        assertThat(probe.branchAction(centro)).isEqualTo("ok");
        assertThatThrownBy(() -> probe.branchAction(norte)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void customersWithoutMembershipCannotActOnTheBusiness() {
        signInAs(newUser(PlatformRole.USER));

        assertThatThrownBy(() -> probe.barberAction(business)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> probe.branchAction(centro)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void anOwnerOfAnotherBusinessCannotActOnThisOne() {
        var otherOwner = api.registerNewUser("Otra dueña");
        api.createBusiness(otherOwner, "otro-" + UUID.randomUUID().toString().substring(0, 8));
        signInAs(principal(otherOwner, PlatformRole.USER));

        assertThatThrownBy(() -> probe.barberAction(business)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> probe.branchAction(norte)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void platformAdminsInheritUserPermissionsButNeedAReasonForBusinesses() {
        signInAs(newUser(PlatformRole.ADMIN));

        assertThat(probe.userAction()).isEqualTo("ok");
        // Fuera de una solicitud HTTP no hay motivo de soporte: el acceso al negocio se niega.
        assertThatThrownBy(() -> probe.barberAction(business)).isInstanceOf(AccessDeniedException.class);
    }

    private AuthenticatedUser member(BusinessRole role, UUID branch) {
        var user = newUser(PlatformRole.USER);
        TenantContext.callInBusiness(
                business,
                () -> transaction.execute(status ->
                        memberships.save(MembershipFixtures.staff(user.id(), business, role, Set.of(branch)))));
        return user;
    }

    private AuthenticatedUser newUser(PlatformRole role) {
        return principal(api.registerNewUser("Persona"), role);
    }

    private static AuthenticatedUser principal(ApiClient.Session session, PlatformRole role) {
        return new AuthenticatedUser(session.userId(), new Email(session.email()), "Persona", role);
    }

    private static void signInAs(AuthenticatedUser user) {
        var authority = new SimpleGrantedAuthority("ROLE_" + user.platformRole().name());
        SecurityContextHolder.getContext()
                .setAuthentication(UsernamePasswordAuthenticationToken.authenticated(user, null, List.of(authority)));
    }

    /** Servicio de prueba con las exigencias de permisos que usan los módulos. */
    static class BusinessProbe {

        @BusinessScoped
        @PreAuthorize("hasPermission(#businessId, 'Business', 'BARBER')")
        public String barberAction(@BusinessId UUID businessId) {
            return "ok";
        }

        @BusinessScoped
        @PreAuthorize("hasPermission(#businessId, 'Business', 'MANAGER')")
        public String managerAction(@BusinessId UUID businessId) {
            return "ok";
        }

        @PreAuthorize("hasPermission(#branchId, 'Branch', 'BARBER')")
        public String branchAction(UUID branchId) {
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
