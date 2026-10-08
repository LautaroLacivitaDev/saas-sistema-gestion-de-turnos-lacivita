package com.lacivita.turnos.shared.security;

import java.io.Serializable;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;

/**
 * Único punto donde se decide si una persona puede actuar sobre un negocio.
 *
 * <p>Se usa desde {@code @PreAuthorize}:
 *
 * <pre>{@code @PreAuthorize("hasPermission(#businessId, 'Business', 'MANAGER')")}</pre>
 *
 * <p>El permiso es el rol mínimo exigido ({@code OWNER}, {@code MANAGER} o {@code BARBER}). Un rol
 * superior incluye a los inferiores. Un {@link PlatformRole#ADMIN} tiene acceso a todos los negocios.
 */
// DECISIÓN: el acceso de un ADMIN a un negocio todavía no se audita. El registro de auditoría llega
// en el Hito 3 y este evaluador es el lugar donde se va a registrar.
class BusinessPermissionEvaluator implements PermissionEvaluator {

    static final String BUSINESS = "Business";

    private final BusinessRoleResolver roles;

    BusinessPermissionEvaluator(BusinessRoleResolver roles) {
        this.roles = roles;
    }

    @Override
    public boolean hasPermission(Authentication authentication, Object targetDomainObject, Object permission) {
        // Solo se admiten permisos por id de recurso, para que cada chequeo diga sobre qué negocio actúa.
        return false;
    }

    @Override
    public boolean hasPermission(
            Authentication authentication, Serializable targetId, String targetType, Object permission) {
        if (!(authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            return false;
        }
        if (!BUSINESS.equals(targetType) || !(targetId instanceof UUID businessId)) {
            throw new IllegalArgumentException("Tipo de recurso no soportado: " + targetType);
        }
        if (user.isAdmin()) {
            return true;
        }
        BusinessRole required = parseRole(permission);
        return roles.roleOf(user.id(), businessId)
                .map(role -> role.includes(required))
                .orElse(false);
    }

    private static BusinessRole parseRole(Object permission) {
        if (permission instanceof BusinessRole role) {
            return role;
        }
        return BusinessRole.valueOf(String.valueOf(permission).toUpperCase(Locale.ROOT));
    }
}
