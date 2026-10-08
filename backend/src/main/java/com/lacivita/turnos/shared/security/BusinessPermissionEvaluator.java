package com.lacivita.turnos.shared.security;

import java.io.Serializable;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

/**
 * Único punto donde se decide si una persona puede actuar sobre un negocio o una sucursal.
 *
 * <p>Se usa desde {@code @PreAuthorize}, con el rol mínimo exigido ({@code OWNER}, {@code MANAGER} o
 * {@code BARBER}):
 *
 * <pre>{@code
 * @PreAuthorize("hasPermission(#businessId, 'Business', 'MANAGER')")
 * @PreAuthorize("hasPermission(#branchId, 'Branch', 'BARBER')")
 * }</pre>
 *
 * <ul>
 *   <li>Un rol superior incluye a los inferiores.
 *   <li>En una sucursal, el gerente y el barbero solo pueden actuar si la tienen asignada. El dueño, en
 *       todas.
 *   <li>Un {@link PlatformRole#ADMIN} que no es miembro entra solo como soporte: con un motivo, que queda
 *       registrado (ver {@link AuditedSupportAccess}).
 * </ul>
 */
class BusinessPermissionEvaluator implements PermissionEvaluator {

    static final String BUSINESS = "Business";
    static final String BRANCH = "Branch";

    private final BusinessMembershipResolver memberships;
    private final BranchLocator branches;
    private final SupportAccess supportAccess;

    BusinessPermissionEvaluator(
            BusinessMembershipResolver memberships, BranchLocator branches, SupportAccess supportAccess) {
        this.memberships = memberships;
        this.branches = branches;
        this.supportAccess = supportAccess;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        // Solo se admiten permisos por id de recurso, para que cada chequeo diga sobre qué actúa.
        return false;
    }

    @Override
    public boolean hasPermission(
            Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (!(authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return false;
        }
        if (!(targetId instanceof UUID id)) {
            return false;
        }
        BusinessRole required = parseRole(permission);
        return switch (targetType) {
            case BUSINESS -> canActOnBusiness(user, id, required);
            case BRANCH -> canActOnBranch(user, id, required);
            default -> throw new IllegalArgumentException("Tipo de recurso no soportado: " + targetType);
        };
    }

    private boolean canActOnBusiness(AuthenticatedUser user, UUID businessId, BusinessRole required) {
        boolean asMember = memberships
                .membershipOf(user.id(), businessId)
                .map(membership -> membership.role().includes(required))
                .orElse(false);
        return asMember || supportAccess.grant(user, businessId);
    }

    private boolean canActOnBranch(AuthenticatedUser user, UUID branchId, BusinessRole required) {
        var businessId = branches.businessOf(branchId);
        if (businessId.isEmpty()) {
            return false;
        }
        boolean asMember = memberships
                .membershipOf(user.id(), businessId.get())
                .map(membership -> membership.role().includes(required) && membership.covers(branchId))
                .orElse(false);
        return asMember || supportAccess.grant(user, businessId.get());
    }

    private static BusinessRole parseRole(Object permission) {
        if (permission instanceof BusinessRole role) {
            return role;
        }
        return BusinessRole.valueOf(String.valueOf(permission).toUpperCase(Locale.ROOT));
    }
}
