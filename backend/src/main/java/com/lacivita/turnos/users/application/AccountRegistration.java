package com.lacivita.turnos.users.application;

import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.users.domain.EmailAlreadyRegisteredException;
import com.lacivita.turnos.users.domain.NewPassword;
import com.lacivita.turnos.users.domain.PasswordHasher;
import com.lacivita.turnos.users.domain.TokenPurpose;
import com.lacivita.turnos.users.domain.User;
import com.lacivita.turnos.users.domain.UserRepository;
import java.time.Clock;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Alta de cuentas con email y contraseña. */
@Service
public class AccountRegistration {

    /** Restricción única del email en {@code user_account} (migración V3). */
    private static final String EMAIL_UNIQUE_CONSTRAINT = "user_account_email_uk";

    private final UserRepository users;
    private final PasswordHasher passwordHasher;
    private final TokenIssuer tokens;
    private final AccountEmails emails;
    private final AccountMapper mapper;
    private final Clock clock;

    AccountRegistration(
            UserRepository users,
            PasswordHasher passwordHasher,
            TokenIssuer tokens,
            AccountEmails emails,
            AccountMapper mapper,
            Clock clock) {
        this.users = users;
        this.passwordHasher = passwordHasher;
        this.tokens = tokens;
        this.emails = emails;
        this.mapper = mapper;
        this.clock = clock;
    }

    /**
     * Crea la cuenta, envía el link de verificación y devuelve la sesión iniciada. El email queda sin
     * verificar hasta que la persona use el link.
     */
    // DECISIÓN: si el email ya existe se responde 409 con un mensaje claro. Facilita saber si un email
    // está registrado, pero el límite de intentos por IP acota ese uso y la experiencia es mejor.
    @Transactional
    public SignIn register(String name, Email email, NewPassword password) {
        if (users.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }
        var user = User.registerWithPassword(name, email, password, passwordHasher, clock.instant());
        save(user);
        emails.sendEmailVerification(user, tokens.issue(user.getId(), TokenPurpose.EMAIL_VERIFICATION));
        return mapper.toSignIn(user);
    }

    private void save(User user) {
        try {
            users.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            // Dos registros simultáneos con el mismo email: la restricción única de la base decide.
            if (violates(ex, EMAIL_UNIQUE_CONSTRAINT)) {
                throw new EmailAlreadyRegisteredException();
            }
            throw ex;
        }
    }

    private static boolean violates(DataIntegrityViolationException ex, String constraint) {
        return ex.getMostSpecificCause().getMessage() != null
                && ex.getMostSpecificCause().getMessage().contains(constraint);
    }
}
