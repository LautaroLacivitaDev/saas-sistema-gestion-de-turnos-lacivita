package com.lacivita.turnos.shared.tenancy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lacivita.turnos.ApiClient;
import com.lacivita.turnos.IntegrationTest;
import com.lacivita.turnos.business.domain.BranchRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Un negocio nunca ve ni modifica datos de otro. Se prueban las dos barreras por separado, directo
 * contra la base y sin pasar por los permisos de la API:
 *
 * <ol>
 *   <li>Row Level Security en PostgreSQL, con consultas SQL a mano.
 *   <li>El filtro automático de Hibernate, con un repositorio.
 * </ol>
 */
@IntegrationTest
class TenantIsolationIntegrationTests {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcClient jdbc;

    @Autowired
    BranchRepository branches;

    @Autowired
    TransactionTemplate transaction;

    UUID businessA;
    UUID businessB;
    UUID branchOfA;

    @BeforeEach
    void twoBusinessesWithTheirOwners() {
        var api = new ApiClient(mvc);
        var ownerA = api.registerNewUser("Dueña A");
        var ownerB = api.registerNewUser("Dueño B");
        businessA = api.createBusiness(
                ownerA, "aislado-a-" + UUID.randomUUID().toString().substring(0, 8));
        businessB = api.createBusiness(
                ownerB, "aislado-b-" + UUID.randomUUID().toString().substring(0, 8));
        branchOfA = api.createBranch(ownerA, businessA, "Sucursal de A");
    }

    @Test
    void theApplicationConnectsWithoutPrivilegesThatSkipRowLevelSecurity() {
        var role = jdbc.sql("SELECT rolsuper, rolbypassrls FROM pg_roles WHERE rolname = current_user")
                .query((row, n) -> new boolean[] {row.getBoolean(1), row.getBoolean(2)})
                .single();

        assertThat(role[0]).as("superusuario").isFalse();
        assertThat(role[1]).as("BYPASSRLS").isFalse();
    }

    @Test
    void withoutContextProtectedRowsAreInvisible() {
        // Falla cerrado: sin negocio ni persona en el contexto, las membresías no se ven.
        assertThat(countMemberships(businessA)).isZero();
    }

    @Test
    void insideABusinessOnlyItsRowsAreVisible() {
        TenantContext.callInBusiness(businessB, () -> {
            assertThat(countMemberships(businessA)).isZero();
            assertThat(countMemberships(businessB)).isEqualTo(1);
            return null;
        });
    }

    @Test
    void insideABusinessRowsOfAnotherCannotBeModified() {
        TenantContext.callInBusiness(businessB, () -> {
            int updated = jdbc.sql("UPDATE branch SET name = 'Hackeada' WHERE id = :id")
                    .param("id", branchOfA)
                    .update();
            assertThat(updated).isZero();

            assertThatThrownBy(() -> jdbc.sql("""
                            INSERT INTO membership (id, user_id, business_id, role, created_at)
                            SELECT :id, user_id, :target, 'OWNER', now() FROM membership LIMIT 1
                            """)
                            .param("id", UUID.randomUUID())
                            .param("target", businessA)
                            .update())
                    .isInstanceOf(DataAccessException.class)
                    .rootCause()
                    .hasMessageContaining("row-level security");
            return null;
        });
    }

    @Test
    void hibernateFiltersEntitiesOfOtherBusinessesAutomatically() {
        // La lectura de sucursales es pública en la base (página de reservas): acá protege el filtro de Hibernate.
        var visibleFromB = TenantContext.callInBusiness(
                businessB,
                () -> transaction.execute(status -> branches.findAllByBusinessIdOrderByCreatedAtAsc(businessA)));
        var visibleFromA = TenantContext.callInBusiness(
                businessA,
                () -> transaction.execute(status -> branches.findAllByBusinessIdOrderByCreatedAtAsc(businessA)));

        assertThat(visibleFromB).isEmpty();
        assertThat(visibleFromA).hasSize(1);
    }

    @Test
    void systemOperationsSeeEveryBusiness() {
        long total = TenantContext.callAsSystem(
                "prueba de aislamiento", () -> countMemberships(businessA) + countMemberships(businessB));

        assertThat(total).isEqualTo(2);
    }

    private long countMemberships(UUID businessId) {
        return jdbc.sql("SELECT count(*) FROM membership WHERE business_id = :businessId")
                .param("businessId", businessId)
                .query(Long.class)
                .single();
    }
}
