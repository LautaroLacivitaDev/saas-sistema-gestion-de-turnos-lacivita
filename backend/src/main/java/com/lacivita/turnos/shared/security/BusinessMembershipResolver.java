package com.lacivita.turnos.shared.security;

import java.util.Optional;
import java.util.UUID;

/**
 * Consulta la membresía de una persona en un negocio. Lo implementa el módulo de usuarios, dueño de las
 * membresías; la seguridad depende solo de esta interfaz y no del módulo.
 */
public interface BusinessMembershipResolver {

    Optional<BusinessMembership> membershipOf(UUID userId, UUID businessId);
}
