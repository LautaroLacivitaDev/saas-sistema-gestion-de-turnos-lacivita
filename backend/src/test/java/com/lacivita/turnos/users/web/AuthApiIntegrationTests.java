package com.lacivita.turnos.users.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.lacivita.turnos.IntegrationTest;
import com.lacivita.turnos.RecordingMailer;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@IntegrationTest
class AuthApiIntegrationTests {

    static final String PASSWORD = "una-clave-segura";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    RecordingMailer mailer;

    @Nested
    class Registration {

        @Test
        void createsTheAccountStartsTheSessionAndSendsTheVerificationLink() {
            String email = uniqueEmail();

            var result = register("Ana Pérez", email, PASSWORD);

            assertThat(result).hasStatus(HttpStatus.CREATED);
            assertThat(result).bodyJson().extractingPath("$.email").isEqualTo(email);
            assertThat(result).bodyJson().extractingPath("$.emailVerified").isEqualTo(false);
            assertThat(result).bodyJson().extractingPath("$.hasPassword").isEqualTo(true);
            assertThat(result).bodyText().doesNotContain(PASSWORD);
            assertThat(me(sessionOf(result))).hasStatusOk();
            assertThat(mailer.lastTokenSentTo(email)).isPresent();
        }

        @Test
        void theSameEmailCannotBeRegisteredTwiceEvenWithDifferentCase() {
            String email = uniqueEmail();
            register("Ana", email, PASSWORD);

            var result = register("Otra Ana", email.toUpperCase(), PASSWORD);

            assertThat(result).hasStatus(HttpStatus.CONFLICT);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("email_already_registered");
        }

        @Test
        void invalidDataIsReportedPerField() {
            var result = register("", "no-es-un-email", "corta");

            assertThat(result).hasStatus(HttpStatus.BAD_REQUEST).hasContentType("application/problem+json");
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("validation_failed");
            assertThat(result).bodyJson().extractingPath("$.errors.name").isNotNull();
            assertThat(result).bodyJson().extractingPath("$.errors.email").isNotNull();
            assertThat(result).bodyJson().extractingPath("$.errors.password").isNotNull();
        }

        @Test
        void requestsWithoutCsrfTokenAreRejected() {
            var result = mvc.post()
                    .uri("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(registerJson("Ana", uniqueEmail(), PASSWORD))
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.FORBIDDEN);
        }
    }

    @Nested
    class PasswordLogin {

        @Test
        void validCredentialsStartASession() {
            String email = uniqueEmail();
            register("Ana", email, PASSWORD);

            var result = login(email, PASSWORD);

            assertThat(result).hasStatusOk();
            assertThat(me(sessionOf(result)))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.email")
                    .isEqualTo(email);
        }

        @Test
        void theEmailIsCaseInsensitive() {
            String email = uniqueEmail();
            register("Ana", email, PASSWORD);

            assertThat(login(email.toUpperCase(), PASSWORD)).hasStatusOk();
        }

        @Test
        void aWrongPasswordAndAnUnknownEmailGetTheSameAnswer() {
            String email = uniqueEmail();
            register("Ana", email, PASSWORD);

            var wrongPassword = login(email, "otra-clave-cualquiera");
            var unknownEmail = login(uniqueEmail(), PASSWORD);

            for (var result : new MvcTestResult[] {wrongPassword, unknownEmail}) {
                assertThat(result).hasStatus(HttpStatus.UNAUTHORIZED);
                assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("invalid_credentials");
            }
        }

        @Test
        void loggingOutEndsTheSession() {
            String email = uniqueEmail();
            var session = sessionOf(register("Ana", email, PASSWORD));

            var logout = mvc.post()
                    .uri("/api/auth/logout")
                    .cookie(session)
                    .with(csrf())
                    .exchange();

            assertThat(logout).hasStatus(HttpStatus.NO_CONTENT);
            assertThat(me(session)).hasStatus(HttpStatus.UNAUTHORIZED);
        }

        @Test
        void loggingInIssuesANewSessionId() {
            String email = uniqueEmail();
            var registrationSession = sessionOf(register("Ana", email, PASSWORD));

            var result = mvc.post()
                    .uri("/api/auth/login")
                    .cookie(registrationSession)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(loginJson(email, PASSWORD))
                    .with(csrf())
                    .exchange();

            assertThat(sessionOf(result).getValue()).isNotEqualTo(registrationSession.getValue());
        }
    }

    @Nested
    class EmailVerification {

        @Test
        void theLinkFromTheEmailVerifiesTheAccount() {
            String email = uniqueEmail();
            var session = sessionOf(register("Ana", email, PASSWORD));
            String token = mailer.lastTokenSentTo(email).orElseThrow();

            assertThat(postToken("/api/auth/email-verification", token)).hasStatus(HttpStatus.NO_CONTENT);

            assertThat(me(session)).bodyJson().extractingPath("$.emailVerified").isEqualTo(true);
        }

        @Test
        void theLinkWorksOnlyOnce() {
            String email = uniqueEmail();
            register("Ana", email, PASSWORD);
            String token = mailer.lastTokenSentTo(email).orElseThrow();
            postToken("/api/auth/email-verification", token);

            var secondUse = postToken("/api/auth/email-verification", token);

            assertThat(secondUse).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
            assertThat(secondUse).bodyJson().extractingPath("$.code").isEqualTo("invalid_token");
        }

        @Test
        void anInventedTokenIsRejected() {
            assertThat(postToken("/api/auth/email-verification", "token-inventado"))
                    .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        }

        @Test
        void aLoggedInUserCanAskForANewLink() {
            String email = uniqueEmail();
            var session = sessionOf(register("Ana", email, PASSWORD));

            var result = mvc.post()
                    .uri("/api/auth/email-verification/resend")
                    .cookie(session)
                    .with(csrf())
                    .exchange();

            assertThat(result).hasStatus(HttpStatus.ACCEPTED);
            assertThat(mailer.sentTo(email)).hasSize(2);
        }
    }

    @Nested
    class LoginLinks {

        @Test
        void theLinkStartsASessionAndVerifiesTheEmail() {
            String email = uniqueEmail();
            register("Ana", email, PASSWORD);

            assertThat(requestLoginLink(email)).hasStatus(HttpStatus.ACCEPTED);
            String token = mailer.lastTokenSentTo(email).orElseThrow();
            var result = postToken("/api/auth/login-link/consume", token);

            assertThat(result).hasStatusOk();
            assertThat(result).bodyJson().extractingPath("$.emailVerified").isEqualTo(true);
            assertThat(me(sessionOf(result))).hasStatusOk();
        }

        @Test
        void unknownEmailsGetTheSameAnswerAndNoEmailIsSent() {
            String email = uniqueEmail();

            assertThat(requestLoginLink(email)).hasStatus(HttpStatus.ACCEPTED);
            assertThat(mailer.sentTo(email)).isEmpty();
        }

        @Test
        void aVerificationTokenCannotBeUsedToLogIn() {
            String email = uniqueEmail();
            register("Ana", email, PASSWORD);
            String verificationToken = mailer.lastTokenSentTo(email).orElseThrow();

            assertThat(postToken("/api/auth/login-link/consume", verificationToken))
                    .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        }
    }

    @Test
    void theCurrentAccountRequiresASession() {
        assertThat(mvc.get().uri("/api/auth/me")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void theCsrfEndpointHandsOutAToken() {
        assertThat(mvc.get().uri("/api/auth/csrf"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.headerName")
                .isEqualTo("X-XSRF-TOKEN");
    }

    // --- Ayudantes ---

    private MvcTestResult register(String name, String email, String password) {
        return mvc.post()
                .uri("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(registerJson(name, email, password))
                .with(csrf())
                .exchange();
    }

    private MvcTestResult login(String email, String password) {
        return mvc.post()
                .uri("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(loginJson(email, password))
                .with(csrf())
                .exchange();
    }

    private MvcTestResult requestLoginLink(String email) {
        return mvc.post()
                .uri("/api/auth/login-link")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\"}".formatted(email))
                .with(csrf())
                .exchange();
    }

    private MvcTestResult postToken(String uri, String token) {
        return mvc.post()
                .uri(uri)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"token\":\"%s\"}".formatted(token))
                .with(csrf())
                .exchange();
    }

    private MvcTestResult me(Cookie session) {
        return mvc.get().uri("/api/auth/me").cookie(session).exchange();
    }

    private static Cookie sessionOf(MvcTestResult result) {
        Cookie cookie = result.getResponse().getCookie("SESSION");
        assertThat(cookie).as("cookie de sesión").isNotNull();
        // Spring Session escribe el encabezado Set-Cookie a mano: los atributos se verifican ahí.
        assertThat(result.getResponse().getHeaders("Set-Cookie"))
                .filteredOn(header -> header.startsWith("SESSION="))
                .singleElement()
                .asString()
                .contains("HttpOnly", "SameSite=Lax", "Path=/");
        return cookie;
    }

    private static String registerJson(String name, String email, String password) {
        return """
                {"name":"%s","email":"%s","password":"%s"}""".formatted(name, email, password);
    }

    private static String loginJson(String email, String password) {
        return """
                {"email":"%s","password":"%s"}""".formatted(email, password);
    }

    private static String uniqueEmail() {
        return "persona-" + UUID.randomUUID() + "@example.com";
    }
}
