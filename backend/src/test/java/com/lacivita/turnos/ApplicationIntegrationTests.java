package com.lacivita.turnos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** Arranca la aplicación completa contra PostgreSQL real y verifica la base del proyecto. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ApplicationIntegrationTests {

    @Autowired
    JdbcClient jdbc;

    @Autowired
    MockMvcTester mvc;

    @Test
    void migrationsInstallRequiredExtensions() {
        var extensions =
                jdbc.sql("SELECT extname FROM pg_extension").query(String.class).list();

        assertThat(extensions).contains("btree_gist", "pg_trgm", "unaccent");
    }

    @Test
    void healthEndpointIsUp() {
        assertThat(mvc.get().uri("/actuator/health"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.status")
                .isEqualTo("UP");
    }

    @Test
    void openApiDocumentIsPublished() {
        assertThat(mvc.get().uri("/api/openapi")).hasStatusOk();
    }

    @Test
    void unknownRouteReturnsProblemDetails() {
        assertThat(mvc.get().uri("/api/no-existe"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .hasContentType("application/problem+json");
    }
}
