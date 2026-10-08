package com.lacivita.turnos.users.domain;

import com.lacivita.turnos.shared.domain.Ids;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Token de un solo uso enviado por email (verificación de email o link de acceso). Guarda solo el hash
 * del secreto, vence según su {@link TokenPurpose} y no se puede usar dos veces.
 */
@Entity
@Table(name = "auth_token")
public class AuthToken {

    @Id
    private UUID id;

    private UUID userId;

    @Enumerated(EnumType.STRING)
    private TokenPurpose purpose;

    private String tokenHash;

    private Instant expiresAt;

    private Instant usedAt;

    private Instant createdAt;

    protected AuthToken() {
        // Requerido por JPA.
    }

    private AuthToken(UUID userId, TokenPurpose purpose, TokenSecret secret, Instant now) {
        this.id = Ids.newId();
        this.userId = Objects.requireNonNull(userId, "userId");
        this.purpose = Objects.requireNonNull(purpose, "purpose");
        this.tokenHash = secret.hash();
        this.createdAt = now;
        this.expiresAt = now.plus(purpose.validity());
    }

    public static AuthToken issue(UUID userId, TokenPurpose purpose, TokenSecret secret, Instant now) {
        return new AuthToken(userId, purpose, secret, now);
    }

    /**
     * Usa el token y devuelve el id del usuario al que pertenece.
     *
     * @throws InvalidTokenException si es de otro tipo, ya se usó o venció
     */
    public UUID consume(TokenPurpose expectedPurpose, Instant now) {
        if (purpose != expectedPurpose || usedAt != null || !now.isBefore(expiresAt)) {
            throw new InvalidTokenException();
        }
        usedAt = now;
        return userId;
    }
}
