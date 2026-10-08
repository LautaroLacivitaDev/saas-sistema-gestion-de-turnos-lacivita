package com.lacivita.turnos.users.application;

import com.lacivita.turnos.shared.audit.AuditableEvent;
import com.lacivita.turnos.shared.security.BusinessRole;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** Cambios en el equipo de un negocio. Por ahora solo los consume el registro de auditoría. */
final class TeamEvents {

    private TeamEvents() {}

    /** Base común: todos los eventos del equipo pertenecen a un negocio y apuntan a una persona o invitación. */
    private interface TeamEvent extends AuditableEvent {

        UUID businessId();

        @Override
        default Optional<UUID> auditBusinessId() {
            return Optional.of(businessId());
        }
    }

    record MemberInvited(UUID businessId, UUID invitationId, String email, BusinessRole role, Set<UUID> branchIds)
            implements TeamEvent {

        @Override
        public String auditAction() {
            return "team.member_invited";
        }

        @Override
        public String auditEntityType() {
            return "Invitation";
        }

        @Override
        public String auditEntityId() {
            return invitationId.toString();
        }

        @Override
        public Optional<Object> auditAfter() {
            return Optional.of(Map.of("email", email, "role", role, "branchIds", branchIds));
        }
    }

    record InvitationRevoked(UUID businessId, UUID invitationId) implements TeamEvent {

        @Override
        public String auditAction() {
            return "team.invitation_revoked";
        }

        @Override
        public String auditEntityType() {
            return "Invitation";
        }

        @Override
        public String auditEntityId() {
            return invitationId.toString();
        }
    }

    record MemberJoined(UUID businessId, UUID userId, BusinessRole role, Set<UUID> branchIds) implements TeamEvent {

        @Override
        public String auditAction() {
            return "team.member_joined";
        }

        @Override
        public String auditEntityType() {
            return "Membership";
        }

        @Override
        public String auditEntityId() {
            return userId.toString();
        }

        @Override
        public Optional<Object> auditAfter() {
            return Optional.of(Map.of("role", role, "branchIds", branchIds));
        }
    }

    record MemberRoleChanged(UUID businessId, UUID userId, BusinessRole before, BusinessRole after)
            implements TeamEvent {

        @Override
        public String auditAction() {
            return "team.role_changed";
        }

        @Override
        public String auditEntityType() {
            return "Membership";
        }

        @Override
        public String auditEntityId() {
            return userId.toString();
        }

        @Override
        public Optional<Object> auditBefore() {
            return Optional.of(Map.of("role", before));
        }

        @Override
        public Optional<Object> auditAfter() {
            return Optional.of(Map.of("role", after));
        }
    }

    record MemberBranchesChanged(UUID businessId, UUID userId, Set<UUID> before, Set<UUID> after) implements TeamEvent {

        @Override
        public String auditAction() {
            return "team.branches_changed";
        }

        @Override
        public String auditEntityType() {
            return "Membership";
        }

        @Override
        public String auditEntityId() {
            return userId.toString();
        }

        @Override
        public Optional<Object> auditBefore() {
            return Optional.of(Map.of("branchIds", before));
        }

        @Override
        public Optional<Object> auditAfter() {
            return Optional.of(Map.of("branchIds", after));
        }
    }

    record MemberRemoved(UUID businessId, UUID userId, BusinessRole role) implements TeamEvent {

        @Override
        public String auditAction() {
            return "team.member_removed";
        }

        @Override
        public String auditEntityType() {
            return "Membership";
        }

        @Override
        public String auditEntityId() {
            return userId.toString();
        }

        @Override
        public Optional<Object> auditBefore() {
            return Optional.of(Map.of("role", role));
        }
    }
}
