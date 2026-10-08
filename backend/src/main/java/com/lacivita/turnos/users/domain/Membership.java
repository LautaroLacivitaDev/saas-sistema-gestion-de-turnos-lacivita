package com.lacivita.turnos.users.domain;

import com.lacivita.turnos.shared.domain.Ids;
import com.lacivita.turnos.shared.security.BusinessRole;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Rol de una persona dentro de un negocio. Una persona tiene a lo sumo una membresía por negocio.
 *
 * <p>Las invitaciones y los cambios de rol (con sus reglas: solo el dueño nombra gerentes, el gerente
 * invita barberos) llegan en el Hito 3.
 */
@Entity
@Table(name = "membership")
public class Membership {

    @Id
    private UUID id;

    private UUID userId;

    private UUID businessId;

    @Enumerated(EnumType.STRING)
    private BusinessRole role;

    private Instant createdAt;

    protected Membership() {
        // Requerido por JPA.
    }

    private Membership(UUID userId, UUID businessId, BusinessRole role, Instant now) {
        this.id = Ids.newId();
        this.userId = Objects.requireNonNull(userId, "userId");
        this.businessId = Objects.requireNonNull(businessId, "businessId");
        this.role = Objects.requireNonNull(role, "role");
        this.createdAt = now;
    }

    public static Membership grant(UUID userId, UUID businessId, BusinessRole role, Instant now) {
        return new Membership(userId, businessId, role, now);
    }

    public BusinessRole getRole() {
        return role;
    }
}
