package com.lacivita.turnos.shared.security;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Rol de una persona en un negocio y las sucursales en las que trabaja.
 *
 * @param branchIds sucursales asignadas; el dueño opera en todas y no necesita asignaciones
 */
public record BusinessMembership(BusinessRole role, Set<UUID> branchIds) {

    public BusinessMembership {
        Objects.requireNonNull(role, "role");
        branchIds = Set.copyOf(branchIds);
    }

    /** {@code true} si la persona puede operar en esa sucursal. */
    public boolean covers(UUID branchId) {
        return role == BusinessRole.OWNER || branchIds.contains(branchId);
    }
}
