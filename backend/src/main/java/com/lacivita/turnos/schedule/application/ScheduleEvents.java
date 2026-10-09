package com.lacivita.turnos.schedule.application;

import com.lacivita.turnos.schedule.domain.ScheduleRules;
import com.lacivita.turnos.schedule.domain.WeeklyHours;
import com.lacivita.turnos.shared.audit.AuditableEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Cambios en la agenda. Por ahora solo los consume el registro de auditoría. */
final class ScheduleEvents {

    private ScheduleEvents() {}

    /** Base común: todos los eventos de la agenda pertenecen a un negocio. */
    private interface ScheduleEvent extends AuditableEvent {

        UUID businessId();

        @Override
        default Optional<UUID> auditBusinessId() {
            return Optional.of(businessId());
        }
    }

    record BranchHoursChanged(UUID businessId, UUID branchId, WeeklyHours hours) implements ScheduleEvent {

        @Override
        public String auditAction() {
            return "schedule.branch_hours_changed";
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
            return Optional.of(hours.days());
        }
    }

    record WorkHoursChanged(UUID businessId, UUID barberId, UUID branchId, WeeklyHours hours) implements ScheduleEvent {

        @Override
        public String auditAction() {
            return "schedule.work_hours_changed";
        }

        @Override
        public String auditEntityType() {
            return "Barber";
        }

        @Override
        public String auditEntityId() {
            return barberId.toString();
        }

        @Override
        public Optional<Object> auditAfter() {
            return Optional.of(Map.of("branchId", branchId, "days", hours.days()));
        }
    }

    record HolidayChanged(UUID businessId, UUID holidayId, UUID branchId, LocalDate date, String name, boolean added)
            implements ScheduleEvent {

        @Override
        public String auditAction() {
            return added ? "schedule.holiday_added" : "schedule.holiday_removed";
        }

        @Override
        public String auditEntityType() {
            return "Holiday";
        }

        @Override
        public String auditEntityId() {
            return holidayId.toString();
        }

        @Override
        public Optional<Object> auditBefore() {
            return added ? Optional.empty() : Optional.of(details());
        }

        @Override
        public Optional<Object> auditAfter() {
            return added ? Optional.of(details()) : Optional.empty();
        }

        private Map<String, Object> details() {
            var map = new HashMap<String, Object>();
            map.put("branchId", branchId);
            map.put("date", date.toString());
            map.put("name", name);
            return map;
        }
    }

    record TimeBlockChanged(
            UUID businessId,
            UUID blockId,
            UUID barberId,
            UUID branchId,
            Instant startsAt,
            Instant endsAt,
            boolean added)
            implements ScheduleEvent {

        @Override
        public String auditAction() {
            return added ? "schedule.block_added" : "schedule.block_removed";
        }

        @Override
        public String auditEntityType() {
            return "TimeBlock";
        }

        @Override
        public String auditEntityId() {
            return blockId.toString();
        }

        @Override
        public Optional<Object> auditBefore() {
            return added ? Optional.empty() : Optional.of(details());
        }

        @Override
        public Optional<Object> auditAfter() {
            return added ? Optional.of(details()) : Optional.empty();
        }

        private Map<String, Object> details() {
            var map = new HashMap<String, Object>();
            map.put("barberId", barberId);
            map.put("branchId", branchId);
            map.put("startsAt", startsAt.toString());
            map.put("endsAt", endsAt.toString());
            return map;
        }
    }

    record RulesChanged(UUID businessId, ScheduleRules before, ScheduleRules after) implements ScheduleEvent {

        @Override
        public String auditAction() {
            return "schedule.rules_changed";
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
}
