package com.lacivita.turnos.users.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AuthTokenTests {

    static final Instant NOW = Instant.parse("2026-10-08T12:00:00Z");
    static final UUID USER = UUID.randomUUID();

    @Test
    void aValidTokenReturnsItsOwner() {
        var token = AuthToken.issue(USER, TokenPurpose.LOGIN_LINK, TokenSecret.generate(), NOW);

        assertThat(token.consume(TokenPurpose.LOGIN_LINK, NOW.plusSeconds(60))).isEqualTo(USER);
    }

    @Test
    void aTokenCanOnlyBeUsedOnce() {
        var token = AuthToken.issue(USER, TokenPurpose.LOGIN_LINK, TokenSecret.generate(), NOW);
        token.consume(TokenPurpose.LOGIN_LINK, NOW);

        assertThatThrownBy(() -> token.consume(TokenPurpose.LOGIN_LINK, NOW)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void anExpiredTokenIsRejected() {
        var token = AuthToken.issue(USER, TokenPurpose.LOGIN_LINK, TokenSecret.generate(), NOW);

        var justExpired = NOW.plus(TokenPurpose.LOGIN_LINK.validity());

        assertThatThrownBy(() -> token.consume(TokenPurpose.LOGIN_LINK, justExpired))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void aTokenIsOnlyValidForItsPurpose() {
        var token = AuthToken.issue(USER, TokenPurpose.EMAIL_VERIFICATION, TokenSecret.generate(), NOW);

        assertThatThrownBy(() -> token.consume(TokenPurpose.LOGIN_LINK, NOW)).isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void secretsAreRandomAndOnlyTheirHashIsComparable() {
        var first = TokenSecret.generate();
        var second = TokenSecret.generate();

        assertThat(first).isNotEqualTo(second);
        assertThat(first.hash()).hasSize(64).isEqualTo(new TokenSecret(first.value()).hash());
        assertThat(first.toString()).doesNotContain(first.value());
    }
}
