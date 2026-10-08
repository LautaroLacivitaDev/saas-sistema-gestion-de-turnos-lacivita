package com.lacivita.turnos.users.domain;

import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.shared.domain.Ids;
import com.lacivita.turnos.shared.domain.InvalidValueException;
import com.lacivita.turnos.shared.security.PlatformRole;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Cuenta de una persona en la plataforma. La misma cuenta sirve para reservar como cliente y para
 * trabajar en uno o varios negocios (eso lo definen las membresías).
 *
 * <p>Se puede entrar con contraseña, con un link de acceso por email o con Google. Una cuenta creada con
 * Google o con link de acceso no tiene contraseña.
 */
@Entity
@Table(name = "user_account")
public class User {

    private static final int MAX_NAME_LENGTH = 120;

    @Id
    private UUID id;

    private String name;

    private Email email;

    // El teléfono (columna phone) se incorpora con su objeto de valor cuando se pida en la primera
    // reserva (Hito 6). No se mapea antes para no tener un atributo que nada escribe ni lee.

    private String passwordHash;

    private Instant emailVerifiedAt;

    @Enumerated(EnumType.STRING)
    private PlatformRole platformRole;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<UserIdentity> identities = new HashSet<>();

    private Instant createdAt;

    private Instant updatedAt;

    @Version
    private long version;

    protected User() {
        // Requerido por JPA.
    }

    private User(String name, Email email, Instant now) {
        this.id = Ids.newId();
        this.name = validName(name);
        this.email = Objects.requireNonNull(email, "email");
        this.platformRole = PlatformRole.USER;
        this.createdAt = now;
        this.updatedAt = now;
    }

    /** Alta con email y contraseña. El email queda sin verificar hasta que la persona use el link. */
    public static User registerWithPassword(
            String name, Email email, NewPassword password, PasswordHasher hasher, Instant now) {
        var user = new User(name, email, now);
        user.passwordHash = hasher.hash(password);
        return user;
    }

    /**
     * Alta a partir de un proveedor externo que ya verificó el email (por ejemplo Google). La cuenta
     * nace verificada y sin contraseña.
     */
    public static User registerWithExternalIdentity(
            String name, Email email, IdentityProvider provider, String subject, Instant now) {
        var user = new User(name, email, now);
        user.emailVerifiedAt = now;
        user.linkIdentity(provider, subject, now);
        return user;
    }

    /** Marca el email como verificado. Verificar dos veces no cambia la fecha original. */
    public void verifyEmail(Instant now) {
        if (emailVerifiedAt == null) {
            emailVerifiedAt = now;
            touch(now);
        }
    }

    /**
     * Vincula una identidad externa. Si la cuenta ya tiene otra identidad del mismo proveedor, se
     * rechaza: una cuenta tiene a lo sumo una cuenta de Google.
     */
    public void linkIdentity(IdentityProvider provider, String subject, Instant now) {
        Optional<UserIdentity> existing = identityFor(provider);
        if (existing.isPresent()) {
            if (existing.get().hasSubject(subject)) {
                return;
            }
            throw new IdentityAlreadyLinkedException(provider);
        }
        identities.add(new UserIdentity(this, provider, subject, now));
        touch(now);
    }

    /**
     * Descarta la contraseña. Se usa cuando un proveedor externo demuestra que la persona controla el
     * email de una cuenta que nunca se verificó: quien creó esa cuenta pudo no ser el dueño del email,
     * y su contraseña no debe seguir sirviendo.
     */
    public void discardPassword(Instant now) {
        if (passwordHash != null) {
            passwordHash = null;
            touch(now);
        }
    }

    /**
     * Compara la contraseña ingresada con la de la cuenta. Sin contraseña devuelve {@code false}, pero
     * igual gasta el tiempo de una comparación para no revelar que la cuenta no tiene contraseña.
     */
    public boolean passwordMatches(String rawPassword, PasswordHasher hasher) {
        if (passwordHash == null) {
            hasher.simulateMatch(rawPassword);
            return false;
        }
        return hasher.matches(rawPassword, passwordHash);
    }

    public boolean hasPassword() {
        return passwordHash != null;
    }

    public boolean isEmailVerified() {
        return emailVerifiedAt != null;
    }

    public Optional<UserIdentity> identityFor(IdentityProvider provider) {
        return identities.stream()
                .filter(identity -> identity.getProvider() == provider)
                .findFirst();
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Email getEmail() {
        return email;
    }

    public PlatformRole getPlatformRole() {
        return platformRole;
    }

    private void touch(Instant now) {
        this.updatedAt = now;
    }

    private static String validName(String name) {
        if (name == null || name.isBlank()) {
            throw new InvalidValueException("invalid_name", "El nombre es obligatorio.");
        }
        String trimmed = name.strip();
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new InvalidValueException(
                    "invalid_name", "El nombre puede tener hasta " + MAX_NAME_LENGTH + " caracteres.");
        }
        return trimmed;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof User user && id != null && id.equals(user.id));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
