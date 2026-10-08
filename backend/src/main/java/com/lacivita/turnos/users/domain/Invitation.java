package com.lacivita.turnos.users.domain;

import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.shared.domain.Ids;
import com.lacivita.turnos.shared.security.BusinessRole;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

/**
 * Invitación para sumarse al equipo de un negocio como gerente o barbero. Se envía por email con un link
 * de un solo uso; solo la puede aceptar una cuenta con ese mismo email.
 */
@Entity
@Table(name = "invitation")
public class Invitation {

    static final Duration VALIDITY = Duration.ofDays(7);

    @Id
    private UUID id;

    @TenantId
    private UUID businessId;

    private Email email;

    @Enumerated(EnumType.STRING)
    private BusinessRole role;

    private String tokenHash;

    private UUID invitedBy;

    private Instant expiresAt;

    private Instant acceptedAt;

    private Instant revokedAt;

    private Instant createdAt;

    @ElementCollection
    @CollectionTable(name = "invitation_branch", joinColumns = @JoinColumn(name = "invitation_id"))
    @Column(name = "branch_id")
    private Set<UUID> branchIds = new HashSet<>();

    /** Nulo hasta el primer guardado: así Spring Data distingue un alta (el id lo asignamos nosotros). */
    @Version
    private Long version;

    protected Invitation() {
        // Requerido por JPA.
    }

    private Invitation(
            UUID businessId,
            Email email,
            BusinessRole role,
            Set<UUID> branchIds,
            UUID invitedBy,
            TokenSecret secret,
            Instant now) {
        Membership.requireStaffRole(role);
        this.id = Ids.newId();
        this.businessId = Objects.requireNonNull(businessId, "businessId");
        this.email = Objects.requireNonNull(email, "email");
        this.role = role;
        this.branchIds = new HashSet<>(Membership.requireBranches(branchIds));
        this.invitedBy = Objects.requireNonNull(invitedBy, "invitedBy");
        this.tokenHash = secret.hash();
        this.createdAt = now;
        this.expiresAt = now.plus(VALIDITY);
    }

    public static Invitation issue(
            UUID businessId,
            Email email,
            BusinessRole role,
            Set<UUID> branchIds,
            UUID invitedBy,
            TokenSecret secret,
            Instant now) {
        return new Invitation(businessId, email, role, branchIds, invitedBy, secret, now);
    }

    /**
     * Acepta la invitación y devuelve la membresía nueva.
     *
     * @throws InvitationNoLongerValidException si ya se usó, se revocó o venció
     * @throws InvitationEmailMismatchException si la cuenta que acepta tiene otro email
     */
    public Membership accept(UUID userId, Email accountEmail, Instant now) {
        if (!isPending(now)) {
            throw new InvitationNoLongerValidException();
        }
        if (!email.equals(accountEmail)) {
            throw new InvitationEmailMismatchException();
        }
        this.acceptedAt = now;
        return Membership.join(userId, businessId, role, branchIds, now);
    }

    /** Revocar una invitación ya aceptada o revocada no tiene efecto. */
    public void revoke(Instant now) {
        if (acceptedAt == null && revokedAt == null) {
            this.revokedAt = now;
        }
    }

    public boolean isPending(Instant now) {
        return acceptedAt == null && revokedAt == null && now.isBefore(expiresAt);
    }

    public UUID getId() {
        return id;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public Email getEmail() {
        return email;
    }

    public BusinessRole getRole() {
        return role;
    }

    public Set<UUID> getBranchIds() {
        return Set.copyOf(branchIds);
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof Invitation invitation && id != null && id.equals(invitation.id));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
