package com.lacivita.turnos.users;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Consultas sobre el equipo de un negocio que pueden hacer otros módulos. */
public interface TeamDirectory {

    Optional<TeamMember> member(UUID businessId, UUID userId);

    /** Los miembros entre esas personas. Quien no pertenece al negocio no aparece. */
    List<TeamMember> members(UUID businessId, Collection<UUID> userIds);
}
