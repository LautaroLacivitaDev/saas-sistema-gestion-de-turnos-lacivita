package com.lacivita.turnos.users.domain;

import com.lacivita.turnos.shared.security.BusinessRole;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/**
 * Membresías para pruebas de otros paquetes. En producción un gerente o barbero solo entra aceptando
 * una invitación; las pruebas de permisos necesitan crearlos directamente.
 */
public final class MembershipFixtures {

    private MembershipFixtures() {}

    public static Membership staff(UUID userId, UUID businessId, BusinessRole role, Set<UUID> branchIds) {
        return Membership.join(userId, businessId, role, branchIds, Instant.now());
    }
}
