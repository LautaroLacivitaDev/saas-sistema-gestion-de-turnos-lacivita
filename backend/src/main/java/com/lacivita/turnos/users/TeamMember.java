package com.lacivita.turnos.users;

import com.lacivita.turnos.shared.security.BusinessMembership;
import java.util.UUID;

/** Una persona del equipo de un negocio, con su rol y sus sucursales. */
public record TeamMember(UUID userId, String name, BusinessMembership membership) {}
