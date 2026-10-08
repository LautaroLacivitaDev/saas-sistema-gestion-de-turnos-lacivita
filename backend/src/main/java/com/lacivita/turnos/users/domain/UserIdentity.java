package com.lacivita.turnos.users.domain;

import com.lacivita.turnos.shared.domain.Ids;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Cuenta de un proveedor externo vinculada a un {@link User}. Solo la crea el propio usuario. */
@Entity
@Table(name = "user_identity")
public class UserIdentity {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    private IdentityProvider provider;

    private String subject;

    private Instant linkedAt;

    protected UserIdentity() {
        // Requerido por JPA.
    }

    UserIdentity(User user, IdentityProvider provider, String subject, Instant now) {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("El identificador del proveedor es obligatorio");
        }
        this.id = Ids.newId();
        this.user = Objects.requireNonNull(user, "user");
        this.provider = Objects.requireNonNull(provider, "provider");
        this.subject = subject;
        this.linkedAt = now;
    }

    public IdentityProvider provider() {
        return provider;
    }

    boolean hasSubject(String candidate) {
        return subject.equals(candidate);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof UserIdentity identity && id != null && id.equals(identity.id));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
