package com.lacivita.turnos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** Arranca la aplicación completa contra PostgreSQL real y verifica la base del proyecto. */
@IntegrationTest
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
    void healthEndpointIsPublicAndUp() {
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
    void protectedRoutesRequireASessionAndAnswerWithProblemDetails() {
        assertThat(mvc.get().uri("/api/cualquier-cosa"))
                .hasStatus(HttpStatus.UNAUTHORIZED)
                .hasContentType("application/problem+json")
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("authentication_required");
    }

    @Test
    void routesOutsideTheApiAreRejected() {
        assertThat(mvc.get().uri("/no-es-de-la-api")).hasStatus(HttpStatus.UNAUTHORIZED);
    }
}
