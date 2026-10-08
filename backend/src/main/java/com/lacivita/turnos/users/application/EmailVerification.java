package com.lacivita.turnos.users.application;

import com.lacivita.turnos.users.domain.TokenPurpose;
import com.lacivita.turnos.users.domain.UserRepository;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Verificación del email de una cuenta mediante un link de un solo uso. */
@Service
public class EmailVerification {

    private final UserRepository users;
    private final TokenIssuer tokens;
    private final AccountEmails emails;
    private final Clock clock;

    EmailVerification(UserRepository users, TokenIssuer tokens, AccountEmails emails, Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.emails = emails;
        this.clock = clock;
    }

    @Transactional
    public void verify(String rawToken) {
        UUID userId = tokens.consume(rawToken, TokenPurpose.EMAIL_VERIFICATION);
        users.require(userId).verifyEmail(clock.instant());
    }

    /** Envía un link nuevo. Si el email ya está verificado, no hace nada. */
    @Transactional
    public void resend(UUID userId) {
        var user = users.require(userId);
        if (!user.isEmailVerified()) {
            emails.sendEmailVerification(user, tokens.issue(userId, TokenPurpose.EMAIL_VERIFICATION));
        }
    }
}
