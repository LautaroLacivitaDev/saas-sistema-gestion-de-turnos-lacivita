package com.lacivita.turnos.users.application;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Modelos de lectura del equipo. */
public final class TeamViews {

    private TeamViews() {}

    public record MemberView(UUID userId, String name, String email, String role, Set<UUID> branchIds) {}

    public record InvitationView(UUID id, String email, String role, Set<UUID> branchIds, Instant expiresAt) {}

    /** Un negocio en el que trabaja la persona con sesión iniciada. */
    public record MembershipView(UUID businessId, String businessName, String slug, String role) {}
}
