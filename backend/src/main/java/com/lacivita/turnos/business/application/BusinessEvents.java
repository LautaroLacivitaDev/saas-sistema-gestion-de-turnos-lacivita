package com.lacivita.turnos.business.application;

import com.lacivita.turnos.business.domain.BranchDetails;
import com.lacivita.turnos.business.domain.BusinessProfile;
import com.lacivita.turnos.shared.audit.AuditableEvent;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Eventos internos del módulo de negocios. Por ahora solo los consume el registro de auditoría. */
final class BusinessEvents {

    private BusinessEvents() {}

    record ProfileChanged(UUID businessId, BusinessProfile before, BusinessProfile after) implements AuditableEvent {

        @Override
        public Optional<UUID> auditBusinessId() {
            return Optional.of(businessId);
        }

        @Override
        public String auditAction() {
            return "business.profile_changed";
        }

        @Override
        public String auditEntityType() {
            return "Business";
        }

        @Override
        public String auditEntityId() {
            return businessId.toString();
        }

        @Override
        public Optional<Object> auditBefore() {
            return Optional.of(before);
        }

        @Override
        public Optional<Object> auditAfter() {
            return Optional.of(after);
        }
    }

    record SlugChanged(UUID businessId, String before, String after) implements AuditableEvent {

        @Override
        public Optional<UUID> auditBusinessId() {
            return Optional.of(businessId);
        }

        @Override
        public String auditAction() {
            return "business.slug_changed";
        }

        @Override
        public String auditEntityType() {
            return "Business";
        }

        @Override
        public String auditEntityId() {
            return businessId.toString();
        }

        @Override
        public Optional<Object> auditBefore() {
            return Optional.of(Map.of("slug", before));
        }

        @Override
        public Optional<Object> auditAfter() {
            return Optional.of(Map.of("slug", after));
        }
    }

    record BranchOpened(UUID businessId, UUID branchId, BranchDetails details) implements AuditableEvent {

        @Override
        public Optional<UUID> auditBusinessId() {
            return Optional.of(businessId);
        }

        @Override
        public String auditAction() {
            return "branch.opened";
        }

        @Override
        public String auditEntityType() {
            return "Branch";
        }

        @Override
        public String auditEntityId() {
            return branchId.toString();
        }

        @Override
        public Optional<Object> auditAfter() {
            return Optional.of(details);
        }
    }

    record BranchUpdated(UUID businessId, UUID branchId, BranchDetails before, BranchDetails after)
            implements AuditableEvent {

        @Override
        public Optional<UUID> auditBusinessId() {
            return Optional.of(businessId);
        }

        @Override
        public String auditAction() {
            return "branch.updated";
        }

        @Override
        public String auditEntityType() {
            return "Branch";
        }

        @Override
        public String auditEntityId() {
            return branchId.toString();
        }

        @Override
        public Optional<Object> auditBefore() {
            return Optional.of(before);
        }

        @Override
        public Optional<Object> auditAfter() {
            return Optional.of(after);
        }
    }
}
