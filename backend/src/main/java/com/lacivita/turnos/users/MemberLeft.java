package com.lacivita.turnos.users;

import java.util.UUID;

/**
 * Una persona dejó de pertenecer al equipo de un negocio. Se publica dentro de la transacción de la baja,
 * así los módulos que reaccionan (por ejemplo, el catálogo) lo hacen en la misma transacción.
 */
public record MemberLeft(UUID businessId, UUID userId) {}
