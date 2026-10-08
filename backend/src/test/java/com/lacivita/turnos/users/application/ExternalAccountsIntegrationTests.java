package com.lacivita.turnos.users.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lacivita.turnos.IntegrationTest;
import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.shared.security.ExternalIdentity;
import com.lacivita.turnos.users.domain.ExternalEmailNotVerifiedException;
import com.lacivita.turnos.users.domain.IdentityProvider;
import com.lacivita.turnos.users.domain.NewPassword;
import com.lacivita.turnos.users.domain.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/** Login con Google simulado: se prueba la vinculación de cuentas sin llamar a Google. */
@IntegrationTest
class ExternalAccountsIntegrationTests {

    @Autowired
    ExternalAccounts externalAccounts;

    @Autowired
    AccountRegistration registration;

    @Autowired
    PasswordLogin passwordLogin;

    @Autowired
    UserRepository users;

    @Autowired
    TransactionTemplate transaction;

    @Test
    void aNewGoogleUserGetsAVerifiedAccountWithoutPassword() {
        String email = uniqueEmail();

        var principal = externalAccounts.resolve(google(email, true));

        var user = transaction.execute(status -> users.findById(principal.id()).orElseThrow());
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.hasPassword()).isFalse();
        assertThat(principal.name()).isEqualTo("Ana Google");
    }

    @Test
    void loggingInAgainWithGoogleReturnsTheSameAccount() {
        String email = uniqueEmail();
        var identity = google(email, true);

        var first = externalAccounts.resolve(identity);
        var second = externalAccounts.resolve(identity);

        assertThat(second.id()).isEqualTo(first.id());
    }

    @Test
    void googleIsLinkedToAVerifiedAccountWithTheSameEmailInsteadOfCreatingAnother() {
        String email = uniqueEmail();
        var existing = registration.register("Ana", new Email(email), new NewPassword("clave-segura-1"));
        verify(email);

        var principal = externalAccounts.resolve(google(email.toUpperCase(), true));

        assertThat(principal.id()).isEqualTo(existing.principal().id());
        // La contraseña de una cuenta verificada se conserva.
        assertThat(passwordLogin
                        .authenticate(new Email(email), "clave-segura-1")
                        .principal()
                        .id())
                .isEqualTo(existing.principal().id());
    }

    @Test
    void linkingToAnUnverifiedAccountDiscardsItsPassword() {
        String email = uniqueEmail();
        // Alguien registró el email de Ana sin poder verificarlo.
        var squatter = registration.register("Intruso", new Email(email), new NewPassword("clave-del-intruso"));

        var principal = externalAccounts.resolve(google(email, true));

        assertThat(principal.id()).isEqualTo(squatter.principal().id());
        transaction.executeWithoutResult(status -> {
            var user = users.findById(principal.id()).orElseThrow();
            assertThat(user.isEmailVerified()).isTrue();
            assertThat(user.hasPassword()).isFalse();
            assertThat(user.identityFor(IdentityProvider.GOOGLE)).isPresent();
        });
    }

    @Test
    void anEmailNotVerifiedByGoogleIsRejected() {
        assertThatThrownBy(() -> externalAccounts.resolve(google(uniqueEmail(), false)))
                .isInstanceOf(ExternalEmailNotVerifiedException.class);
    }

    private void verify(String email) {
        // Simula el uso del link de acceso, que verifica el email.
        transaction.executeWithoutResult(
                status -> users.findByEmail(new Email(email)).orElseThrow().verifyEmail(Instant.now()));
    }

    private static ExternalIdentity google(String email, boolean verified) {
        return new ExternalIdentity("GOOGLE", "google-" + UUID.randomUUID(), email, verified, "Ana Google");
    }

    private static String uniqueEmail() {
        return "google-" + UUID.randomUUID() + "@example.com";
    }
}
