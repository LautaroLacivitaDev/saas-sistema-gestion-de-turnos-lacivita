package com.lacivita.turnos.users.web;

import com.lacivita.turnos.users.application.TeamViews.InvitationView;
import com.lacivita.turnos.users.application.TeamViews.MemberView;
import com.lacivita.turnos.users.application.TeamViews.MembershipView;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Respuestas HTTP de la gestión del equipo. */
final class TeamResponses {

    private TeamResponses() {}

    record Member(UUID userId, String name, String email, String role, Set<UUID> branchIds) {

        static Member from(MemberView view) {
            return new Member(view.userId(), view.name(), view.email(), view.role(), view.branchIds());
        }
    }

    record Invitation(UUID id, String email, String role, Set<UUID> branchIds, Instant expiresAt) {

        static Invitation from(InvitationView view) {
            return new Invitation(view.id(), view.email(), view.role(), view.branchIds(), view.expiresAt());
        }
    }

    record Membership(UUID businessId, String businessName, String slug, String role) {

        static Membership from(MembershipView view) {
            return new Membership(view.businessId(), view.businessName(), view.slug(), view.role());
        }
    }
}
