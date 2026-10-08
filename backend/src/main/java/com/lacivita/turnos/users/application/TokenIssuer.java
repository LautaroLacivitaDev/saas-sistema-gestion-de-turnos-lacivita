package com.lacivita.turnos.users.application;

import com.lacivita.turnos.users.domain.AuthToken;
import com.lacivita.turnos.users.domain.AuthTokenRepository;
import com.lacivita.turnos.users.domain.InvalidTokenException;
import com.lacivita.turnos.users.domain.TokenPurpose;
import com.lacivita.turnos.users.domain.TokenSecret;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Emite y consume los tokens de un solo uso. Debe llamarse dentro de una transacción. */
@Component
class TokenIssuer {

    private final AuthTokenRepository tokens;
    private final Clock clock;

    TokenIssuer(AuthTokenRepository tokens, Clock clock) {
        this.tokens = tokens;
        this.clock = clock;
    }

    TokenSecret issue(UUID userId, TokenPurpose purpose) {
        var secret = TokenSecret.generate();
        tokens.save(AuthToken.issue(userId, purpose, secret, clock.instant()));
        return secret;
    }

    /** Devuelve el id del usuario dueño del token y lo marca como usado. */
    UUID consume(String rawToken, TokenPurpose purpose) {
        var secret = new TokenSecret(rawToken);
        return tokens.findByTokenHash(secret.hash())
                .orElseThrow(InvalidTokenException::new)
                .consume(purpose, clock.instant());
    }
}
