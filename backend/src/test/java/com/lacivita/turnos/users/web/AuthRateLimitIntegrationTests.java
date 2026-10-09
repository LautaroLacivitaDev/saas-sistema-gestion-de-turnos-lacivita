package com.lacivita.turnos.users.web;

import static com.lacivita.turnos.SpaCsrf.spaCsrf;
import static org.assertj.core.api.Assertions.assertThat;

import com.lacivita.turnos.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@IntegrationTest
@TestPropertySource(properties = "app.rate-limit.requests-per-minute=3")
class AuthRateLimitIntegrationTests {

    @Autowired
    MockMvcTester mvc;

    @Test
    void tooManyLoginAttemptsFromTheSameAddressAreRejectedForAWhile() {
        for (int attempt = 1; attempt <= 3; attempt++) {
            assertThat(attemptLogin("10.0.0.1")).hasStatus(HttpStatus.UNAUTHORIZED);
        }

        var blocked = attemptLogin("10.0.0.1");

        assertThat(blocked).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(Long.parseLong(blocked.getResponse().getHeader("Retry-After")))
                .isPositive();
        assertThat(blocked).bodyJson().extractingPath("$.code").isEqualTo("too_many_requests");
    }

    @Test
    void theLimitIsCountedPerAddress() {
        for (int attempt = 1; attempt <= 3; attempt++) {
            attemptLogin("10.0.0.2");
        }

        assertThat(attemptLogin("10.0.0.3")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void endpointsWithVariablePartsShareOneCounterPerAddress() {
        // Probar códigos en muchos turnos distintos cuenta como el mismo endpoint.
        for (int attempt = 1; attempt <= 3; attempt++) {
            assertThat(confirmSomeHold("10.0.0.4")).hasStatus(HttpStatus.NOT_FOUND);
        }

        assertThat(confirmSomeHold("10.0.0.4")).hasStatus(HttpStatus.TOO_MANY_REQUESTS);
    }

    private MvcTestResult confirmSomeHold(String remoteAddress) {
        return mvc.post()
                .uri("/api/public/businesses/no-existe/holds/" + UUID.randomUUID() + "/confirm")
                .with(request -> {
                    request.setRemoteAddr(remoteAddress);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"code":"123456"}""")
                .with(spaCsrf())
                .exchange();
    }

    private MvcTestResult attemptLogin(String remoteAddress) {
        return mvc.post()
                .uri("/api/auth/login")
                .with(request -> {
                    request.setRemoteAddr(remoteAddress);
                    return request;
                })
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"nadie@example.com","password":"lo-que-sea"}""")
                .with(spaCsrf())
                .exchange();
    }
}
