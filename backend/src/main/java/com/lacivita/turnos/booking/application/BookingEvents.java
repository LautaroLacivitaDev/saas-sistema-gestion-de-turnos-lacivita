package com.lacivita.turnos.booking.application;

import com.lacivita.turnos.booking.domain.AppointmentStatus;
import com.lacivita.turnos.shared.audit.AuditableEvent;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Cambios en los turnos. Por ahora solo los consume el registro de auditoría; las notificaciones al
 * cliente y al profesional llegan en el Hito 7.
 */
final class BookingEvents {

    private BookingEvents() {}

    /** Base común: todos los eventos son de un turno de un negocio. */
    private interface AppointmentEvent extends AuditableEvent {

        UUID businessId();

        UUID appointmentId();

        @Override
        default Optional<UUID> auditBusinessId() {
            return Optional.of(businessId());
        }

        @Override
        default String auditEntityType() {
            return "Appointment";
        }

        @Override
        default String auditEntityId() {
            return appointmentId().toString();
        }
    }

    record AppointmentBooked(
            UUID businessId, UUID appointmentId, UUID barberId, Instant startsAt, String source, String status)
            implements AppointmentEvent {

        @Override
        public String auditAction() {
            return "appointment.booked";
        }

        @Override
        public Optional<Object> auditAfter() {
            return Optional.of(
                    Map.of("barberId", barberId, "startsAt", startsAt.toString(), "source", source, "status", status));
        }
    }

    record StatusChanged(UUID businessId, UUID appointmentId, AppointmentStatus before, AppointmentStatus after)
            implements AppointmentEvent {

        @Override
        public String auditAction() {
            return "appointment.status_changed";
        }

        @Override
        public Optional<Object> auditBefore() {
            return Optional.of(Map.of("status", before));
        }

        @Override
        public Optional<Object> auditAfter() {
            return Optional.of(Map.of("status", after));
        }
    }

    record Rescheduled(
            UUID businessId,
            UUID appointmentId,
            UUID barberBefore,
            Instant startBefore,
            UUID barberAfter,
            Instant startAfter)
            implements AppointmentEvent {

        @Override
        public String auditAction() {
            return "appointment.rescheduled";
        }

        @Override
        public Optional<Object> auditBefore() {
            return Optional.of(slot(barberBefore, startBefore));
        }

        @Override
        public Optional<Object> auditAfter() {
            return Optional.of(slot(barberAfter, startAfter));
        }

        private static Map<String, Object> slot(UUID barberId, Instant startsAt) {
            var map = new HashMap<String, Object>();
            map.put("barberId", barberId);
            map.put("startsAt", startsAt.toString());
            return map;
        }
    }

    record CancellationPolicyChanged(UUID businessId, int before, int after) implements AuditableEvent {

        @Override
        public Optional<UUID> auditBusinessId() {
            return Optional.of(businessId);
        }

        @Override
        public String auditAction() {
            return "booking.cancellation_policy_changed";
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
            return Optional.of(Map.of("noticeHours", before));
        }

        @Override
        public Optional<Object> auditAfter() {
            return Optional.of(Map.of("noticeHours", after));
        }
    }
}
