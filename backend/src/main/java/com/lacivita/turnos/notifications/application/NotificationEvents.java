package com.lacivita.turnos.notifications.application;

import com.lacivita.turnos.shared.audit.AuditableEvent;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Cambios en cómo avisa el negocio. Solo los registra la auditoría. */
final class NotificationEvents {

    private NotificationEvents() {}

    record RemindersChanged(UUID businessId, List<Integer> before, List<Integer> after) implements AuditableEvent {

        @Override
        public Optional<UUID> auditBusinessId() {
            return Optional.of(businessId);
        }

        @Override
        public String auditAction() {
            return "notifications.reminders_changed";
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
            return Optional.of(Map.of("reminderHours", before));
        }

        @Override
        public Optional<Object> auditAfter() {
            return Optional.of(Map.of("reminderHours", after));
        }
    }

    /** @param subject asunto nuevo; nulo si se volvió al texto de Laciturnos */
    record TemplateChanged(UUID businessId, String type, String subject) implements AuditableEvent {

        @Override
        public Optional<UUID> auditBusinessId() {
            return Optional.of(businessId);
        }

        @Override
        public String auditAction() {
            return subject == null ? "notifications.template_reset" : "notifications.template_changed";
        }

        @Override
        public String auditEntityType() {
            return "MessageTemplate";
        }

        @Override
        public String auditEntityId() {
            return businessId + ":" + type;
        }

        @Override
        public Optional<Object> auditAfter() {
            return subject == null ? Optional.empty() : Optional.of(Map.of("subject", subject));
        }
    }
}
