package com.lacivita.turnos.booking.application;

import com.lacivita.turnos.booking.domain.Appointment;
import com.lacivita.turnos.booking.domain.AppointmentRepository;
import com.lacivita.turnos.booking.domain.SlotNotAvailableException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Ocupa horarios de los profesionales. La última palabra la tiene la restricción de exclusión de la base:
 * si dos personas toman el mismo horario a la vez, una sola lo consigue y la otra recibe un error claro.
 */
@Component
class AppointmentSlots {

    private static final String NO_OVERLAP = "appointment_no_overlap";

    private final AppointmentRepository appointments;

    AppointmentSlots(AppointmentRepository appointments) {
        this.appointments = appointments;
    }

    /** Guarda un turno nuevo. Antes libera los HOLD vencidos del profesional, que ya no ocupan su horario. */
    Appointment take(Appointment appointment, Instant now) {
        appointments.deleteExpiredHolds(appointment.getBarberId(), now);
        try {
            return appointments.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException ex) {
            throw translated(ex);
        }
    }

    /**
     * Libera los HOLD vencidos del profesional. Al mover un turno, se llama antes de cambiarlo: el borrado
     * escribe primero los cambios pendientes.
     */
    void releaseExpiredHolds(UUID barberId, Instant now) {
        appointments.deleteExpiredHolds(barberId, now);
    }

    /** Escribe el horario nuevo de un turno que se movió (de hora o de profesional). */
    void flushMove() {
        try {
            appointments.flush();
        } catch (DataIntegrityViolationException ex) {
            throw translated(ex);
        }
    }

    private static RuntimeException translated(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        return message != null && message.contains(NO_OVERLAP) ? new SlotNotAvailableException() : ex;
    }
}
