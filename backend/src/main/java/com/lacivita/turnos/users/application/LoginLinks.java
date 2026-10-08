package com.lacivita.turnos.users.application;

import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.users.domain.TokenPurpose;
import com.lacivita.turnos.users.domain.UserRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Inicio de sesión sin contraseña, con un link de un solo uso enviado por email. */
@Service
public class LoginLinks {

    private final UserRepository users;
    private final TokenIssuer tokens;
    private final AccountEmails emails;
    private final AccountMapper mapper;
    private final Clock clock;

    LoginLinks(UserRepository users, TokenIssuer tokens, AccountEmails emails, AccountMapper mapper, Clock clock) {
        this.users = users;
        this.tokens = tokens;
        this.emails = emails;
        this.mapper = mapper;
        this.clock = clock;
    }

    /**
     * Envía el link si existe una cuenta con ese email. Si no existe, no hace nada y no lo informa: la
     * respuesta es la misma para no revelar qué emails están registrados.
     */
    @Transactional
    public void request(Email email) {
        users.findByEmail(email)
                .ifPresent(user -> emails.sendLoginLink(user, tokens.issue(user.getId(), TokenPurpose.LOGIN_LINK)));
    }

    /** Usar el link demuestra que la persona controla el email, así que también lo verifica. */
    @Transactional
    public SignIn consume(String rawToken) {
        var userId = tokens.consume(rawToken, TokenPurpose.LOGIN_LINK);
        var user =
                users.findById(userId).orElseThrow(() -> new IllegalStateException("Usuario inexistente: " + userId));
        user.verifyEmail(clock.instant());
        return mapper.toSignIn(user);
    }
}
