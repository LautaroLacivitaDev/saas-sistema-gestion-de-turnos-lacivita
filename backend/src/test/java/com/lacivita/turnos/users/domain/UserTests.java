package com.lacivita.turnos.users.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.shared.domain.InvalidValueException;
import com.lacivita.turnos.shared.security.PlatformRole;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserTests {

    static final Instant NOW = Instant.parse("2026-10-08T12:00:00Z");
    static final Email EMAIL = new Email("ana@example.com");
    static final NewPassword PASSWORD = new NewPassword("clave-segura-1");

    final FakePasswordHasher hasher = new FakePasswordHasher();

    @Test
    void registeringWithPasswordCreatesAnUnverifiedCustomerAccount() {
        var user = User.registerWithPassword("  Ana  ", EMAIL, PASSWORD, hasher, NOW);

        assertThat(user.getId()).isNotNull();
        assertThat(user.getName()).isEqualTo("Ana");
        assertThat(user.hasPassword()).isTrue();
        assertThat(user.isEmailVerified()).isFalse();
        assertThat(user.getPlatformRole()).isEqualTo(PlatformRole.USER);
    }

    @Test
    void theRightPasswordMatchesAndAWrongOneDoesNot() {
        var user = User.registerWithPassword("Ana", EMAIL, PASSWORD, hasher, NOW);

        assertThat(user.passwordMatches("clave-segura-1", hasher)).isTrue();
        assertThat(user.passwordMatches("otra-clave", hasher)).isFalse();
    }

    @Test
    void anAccountWithoutPasswordNeverMatchesButStillSpendsTheComparisonTime() {
        var user = User.registerWithExternalIdentity("Ana", EMAIL, IdentityProvider.GOOGLE, "google-123", NOW);

        assertThat(user.passwordMatches("cualquier-cosa", hasher)).isFalse();
        assertThat(hasher.simulatedMatches()).isEqualTo(1);
    }

    @Test
    void registeringWithAnExternalIdentityCreatesAVerifiedAccountWithoutPassword() {
        var user = User.registerWithExternalIdentity("Ana", EMAIL, IdentityProvider.GOOGLE, "google-123", NOW);

        assertThat(user.isEmailVerified()).isTrue();
        assertThat(user.hasPassword()).isFalse();
        assertThat(user.identityFor(IdentityProvider.GOOGLE)).isPresent();
    }

    @Test
    void aNameIsRequired() {
        assertThatThrownBy(() -> User.registerWithPassword(" ", EMAIL, PASSWORD, hasher, NOW))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void verifyingTwiceKeepsTheAccountVerified() {
        var user = User.registerWithPassword("Ana", EMAIL, PASSWORD, hasher, NOW);

        user.verifyEmail(NOW);
        user.verifyEmail(NOW.plusSeconds(60));

        assertThat(user.isEmailVerified()).isTrue();
    }

    @Test
    void linkingTheSameIdentityAgainIsHarmless() {
        var user = User.registerWithExternalIdentity("Ana", EMAIL, IdentityProvider.GOOGLE, "google-123", NOW);

        user.linkIdentity(IdentityProvider.GOOGLE, "google-123", NOW);

        assertThat(user.identityFor(IdentityProvider.GOOGLE)).isPresent();
    }

    @Test
    void anAccountCannotHaveTwoAccountsFromTheSameProvider() {
        var user = User.registerWithExternalIdentity("Ana", EMAIL, IdentityProvider.GOOGLE, "google-123", NOW);

        assertThatThrownBy(() -> user.linkIdentity(IdentityProvider.GOOGLE, "google-999", NOW))
                .isInstanceOf(IdentityAlreadyLinkedException.class);
    }

    @Test
    void aDiscardedPasswordNoLongerWorks() {
        var user = User.registerWithPassword("Ana", EMAIL, PASSWORD, hasher, NOW);

        user.discardPassword(NOW);

        assertThat(user.hasPassword()).isFalse();
        assertThat(user.passwordMatches("clave-segura-1", hasher)).isFalse();
    }
}
