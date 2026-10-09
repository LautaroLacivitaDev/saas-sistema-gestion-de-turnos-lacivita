package com.lacivita.turnos.booking.application;

import com.lacivita.turnos.booking.AppointmentBooked;
import com.lacivita.turnos.booking.AppointmentEvent;
import com.lacivita.turnos.booking.AppointmentRescheduled;
import com.lacivita.turnos.booking.AppointmentStatusChanged;
import com.lacivita.turnos.booking.domain.Appointment;
import com.lacivita.turnos.booking.domain.AppointmentStatus;
import com.lacivita.turnos.shared.audit.AuditableEvent;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Eventos del módulo. Los cambios de los turnos son parte de su API ({@link AppointmentEvent}): los
 * escuchan la auditoría y las notificaciones. Los demás son internos y solo los registra la auditoría.
 */
final class BookingEvents {

    private BookingEvents() {}

    static AppointmentBooked booked(Appointment appointment) {
        return new AppointmentBooked(
                appointment.getBusinessId(),
                appointment.getId(),
                appointment.getBranchId(),
                appointment.getBarberId(),
                appointment.getStartsAt(),
                appointment.getSource().name(),
                appointment.getStatus().name());
    }

    static AppointmentStatusChanged statusChanged(Appointment appointment, AppointmentStatus before) {
        return new AppointmentStatusChanged(
                appointment.getBusinessId(),
                appointment.getId(),
                before.name(),
                appointment.getStatus().name());
    }

    static AppointmentRescheduled rescheduled(Appointment appointment, UUID barberBefore, Instant startBefore) {
        return new AppointmentRescheduled(
                appointment.getBusinessId(),
                appointment.getId(),
                appointment.getBranchId(),
                barberBefore,
                startBefore,
                appointment.getBarberId(),
                appointment.getStartsAt());
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
