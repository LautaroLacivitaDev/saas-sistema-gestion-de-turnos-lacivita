package com.lacivita.turnos.users.application;

import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.security.ExternalIdentity;
import com.lacivita.turnos.shared.security.ExternalIdentityResolver;
import com.lacivita.turnos.users.domain.ExternalEmailNotVerifiedException;
import com.lacivita.turnos.users.domain.IdentityProvider;
import com.lacivita.turnos.users.domain.User;
import com.lacivita.turnos.users.domain.UserRepository;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cuentas que entran con un proveedor externo (Google).
 *
 * <ol>
 *   <li>Si la identidad ya está vinculada, entra a esa cuenta.
 *   <li>Si existe una cuenta con el mismo email, se vincula a ella en lugar de crear otra.
 *   <li>Si no existe, se crea una cuenta nueva, verificada y sin contraseña.
 * </ol>
 *
 * <p>Solo se acepta un email que el proveedor haya verificado.
 */
@Service
class ExternalAccounts implements ExternalIdentityResolver {

    private final UserRepository users;
    private final AccountMapper mapper;
    private final Clock clock;

    ExternalAccounts(UserRepository users, AccountMapper mapper, Clock clock) {
        this.users = users;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Override
    @Transactional
    public AuthenticatedUser resolve(ExternalIdentity identity) {
        var provider = IdentityProvider.fromName(identity.provider())
                .orElseThrow(() -> new IllegalArgumentException("Proveedor no soportado: " + identity.provider()));
        if (!identity.emailVerified() || identity.email() == null) {
            throw new ExternalEmailNotVerifiedException();
        }
        var email = new Email(identity.email());
        var now = clock.instant();

        var user = users.findByIdentity(provider, identity.subject())
                .or(() -> users.findByEmail(email).map(existing -> linkExisting(existing, provider, identity, now)))
                .orElseGet(() -> users.save(User.registerWithExternalIdentity(
                        displayName(identity, email), email, provider, identity.subject(), now)));
        return mapper.toPrincipal(user);
    }

    private static User linkExisting(User user, IdentityProvider provider, ExternalIdentity identity, Instant now) {
        if (!user.isEmailVerified()) {
            // Quien creó esta cuenta nunca probó controlar el email; el proveedor sí lo probó ahora.
            // Se descarta la contraseña para que quien la haya puesto no pueda seguir entrando.
            user.discardPassword(now);
            user.verifyEmail(now);
        }
        user.linkIdentity(provider, identity.subject(), now);
        return user;
    }

    private static String displayName(ExternalIdentity identity, Email email) {
        if (identity.name() != null && !identity.name().isBlank()) {
            return identity.name();
        }
        return email.value().substring(0, email.value().indexOf('@'));
    }
}
