package com.lacivita.turnos.users.domain;

import com.lacivita.turnos.shared.domain.Ids;
import com.lacivita.turnos.shared.domain.InvalidValueException;
import com.lacivita.turnos.shared.security.BusinessMembership;
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
import java.time.Instant;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.hibernate.annotations.TenantId;

/**
 * Rol de una persona dentro de un negocio y las sucursales en las que trabaja. Una persona tiene a lo
 * sumo una membresía por negocio.
 *
 * <ul>
 *   <li>El dueño nace con el negocio, opera en todas las sucursales y su rol no se cambia ni se quita.
 *   <li>Gerentes y barberos entran por invitación y trabajan en al menos una sucursal.
 * </ul>
 */
@Entity
@Table(name = "membership")
public class Membership {

    @Id
    private UUID id;

    private UUID userId;

    @TenantId
    private UUID businessId;

    @Enumerated(EnumType.STRING)
    private BusinessRole role;

    @ElementCollection
    @CollectionTable(name = "membership_branch", joinColumns = @JoinColumn(name = "membership_id"))
    @Column(name = "branch_id")
    private Set<UUID> branchIds = new HashSet<>();

    private Instant createdAt;

    protected Membership() {
        // Requerido por JPA.
    }

    private Membership(UUID userId, UUID businessId, BusinessRole role, Set<UUID> branchIds, Instant now) {
        this.id = Ids.newId();
        this.userId = Objects.requireNonNull(userId, "userId");
        this.businessId = Objects.requireNonNull(businessId, "businessId");
        this.role = Objects.requireNonNull(role, "role");
        this.branchIds = new HashSet<>(branchIds);
        this.createdAt = now;
    }

    public static Membership grantOwner(UUID userId, UUID businessId, Instant now) {
        return new Membership(userId, businessId, BusinessRole.OWNER, Set.of(), now);
    }

    /** Ingreso de un gerente o barbero (al aceptar una invitación). */
    static Membership join(UUID userId, UUID businessId, BusinessRole role, Set<UUID> branchIds, Instant now) {
        requireStaffRole(role);
        return new Membership(userId, businessId, role, requireBranches(branchIds), now);
    }

    public void changeRole(BusinessRole newRole) {
        if (isOwner()) {
            throw TeamActionNotAllowedException.ownerIsFixed();
        }
        requireStaffRole(newRole);
        this.role = newRole;
    }

    public void assignBranches(Set<UUID> newBranchIds) {
        if (isOwner()) {
            throw TeamActionNotAllowedException.ownerIsFixed();
        }
        this.branchIds.clear();
        this.branchIds.addAll(requireBranches(newBranchIds));
    }

    public boolean isOwner() {
        return role == BusinessRole.OWNER;
    }

    public BusinessMembership toBusinessMembership() {
        return new BusinessMembership(role, branchIds);
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getBusinessId() {
        return businessId;
    }

    public BusinessRole getRole() {
        return role;
    }

    public Set<UUID> getBranchIds() {
        return Set.copyOf(branchIds);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    static void requireStaffRole(BusinessRole role) {
        if (role == null || role == BusinessRole.OWNER) {
            throw new InvalidValueException("invalid_role", "El rol tiene que ser gerente o barbero.");
        }
    }

    static Set<UUID> requireBranches(Set<UUID> branchIds) {
        if (branchIds == null || branchIds.isEmpty()) {
            throw new InvalidValueException("branches_required", "Asigná al menos una sucursal.");
        }
        return Set.copyOf(branchIds);
    }

    @Override
    public boolean equals(Object other) {
        return this == other || (other instanceof Membership membership && id != null && id.equals(membership.id));
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
