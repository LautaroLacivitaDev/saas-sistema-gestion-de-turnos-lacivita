package com.lacivita.turnos.shared.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.lacivita.turnos.ApiClient;
import com.lacivita.turnos.ApiClient.Session;
import com.lacivita.turnos.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/** Un ADMIN entra a un negocio solo explicando por qué, y el dueño lo ve en su registro de auditoría. */
@IntegrationTest
class SupportAccessIntegrationTests {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    JdbcClient jdbc;

    ApiClient api;
    Session owner;
    Session admin;
    UUID businessId;

    @BeforeEach
    void setUp() {
        api = new ApiClient(mvc);
        owner = api.registerNewUser("Dueña");
        businessId = api.createBusiness(
                owner, "soporte-" + UUID.randomUUID().toString().substring(0, 8));
        var person = api.registerNewUser("Soporte");
        jdbc.sql("UPDATE user_account SET platform_role = 'ADMIN' WHERE id = :id")
                .param("id", person.userId())
                .update();
        // El rol de plataforma se toma al iniciar sesión.
        admin = api.login(person);
    }

    @Test
    void withoutAReasonTheAdminIsDenied() {
        assertThat(viewBusinessAsAdmin(null)).hasStatus(HttpStatus.FORBIDDEN);
    }

    @Test
    void withAReasonTheAdminGetsInAndTheOwnerSeesWhyOnce() {
        assertThat(viewBusinessAsAdmin("Ticket #123: no puede cargar sucursales"))
                .hasStatusOk();

        var log = api.get(owner, "/api/businesses/" + businessId + "/audit-log");
        assertThat(log).bodyJson().extractingPath("$.items[0].action").isEqualTo("support.access");
        assertThat(log)
                .bodyJson()
                .extractingPath("$.items[0].reason")
                .isEqualTo("Ticket #123: no puede cargar sucursales");
        assertThat(log)
                .bodyJson()
                .extractingPath("$.items[0].actorUserId")
                .isEqualTo(admin.userId().toString());
        assertThat(log).bodyJson().extractingPath("$.totalItems").isEqualTo(2);
    }

    private MvcTestResult viewBusinessAsAdmin(String reason) {
        var request = mvc.get().uri("/api/businesses/" + businessId).cookie(admin.cookie());
        if (reason != null) {
            request = request.header("X-Support-Reason", reason);
        }
        return request.exchange();
    }
}
