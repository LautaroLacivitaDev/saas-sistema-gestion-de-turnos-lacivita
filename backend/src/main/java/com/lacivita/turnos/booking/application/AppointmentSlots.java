package com.lacivita.turnos.booking.application;

import com.lacivita.turnos.booking.domain.AgendaLocks;
import com.lacivita.turnos.booking.domain.Appointment;
import com.lacivita.turnos.booking.domain.AppointmentRepository;
import com.lacivita.turnos.booking.domain.SlotNotAvailableException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Component;

/**
 * Ocupa horarios de los profesionales. La última palabra la tiene la restricción de exclusión de la base:
 * si dos personas toman el mismo horario a la vez, una sola lo consigue y la otra recibe un error claro.
 *
 * <p>Antes de escribir en la agenda de un profesional se toma su turno ({@link AgendaLocks}): con muchas
 * personas a la vez sobre el mismo horario, sin ese turno PostgreSQL detectaba interbloqueos entre ellas y
 * las hacía esperar hasta agotar las conexiones (lo encontró la prueba de carga de {@code load/}).
 */
@Component
class AppointmentSlots {

    private static final String NO_OVERLAP = "appointment_no_overlap";

    private final AppointmentRepository appointments;
    private final AgendaLocks locks;

    AppointmentSlots(AppointmentRepository appointments, AgendaLocks locks) {
        this.appointments = appointments;
        this.locks = locks;
    }

    /** Guarda un turno nuevo. Antes libera los HOLD vencidos del profesional, que ya no ocupan su horario. */
    Appointment take(Appointment appointment, Instant now) {
        locks.lockAgendaOf(appointment.getBarberId());
        appointments.deleteExpiredHolds(appointment.getBarberId(), now);
        try {
            return appointments.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException ex) {
            throw translated(ex);
        } catch (PessimisticLockingFailureException ex) {
            throw new SlotNotAvailableException();
        }
    }

    /**
     * Libera los HOLD vencidos del profesional. Al mover un turno, se llama antes de cambiarlo: el borrado
     * escribe primero los cambios pendientes. Toma el turno de la agenda de destino.
     */
    void releaseExpiredHolds(UUID barberId, Instant now) {
        locks.lockAgendaOf(barberId);
        appointments.deleteExpiredHolds(barberId, now);
    }

    /** Escribe el horario nuevo de un turno que se movió (de hora o de profesional). */
    void flushMove() {
        try {
            appointments.flush();
        } catch (DataIntegrityViolationException ex) {
            throw translated(ex);
        } catch (PessimisticLockingFailureException ex) {
            throw new SlotNotAvailableException();
        }
    }

    private static RuntimeException translated(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        return message != null && message.contains(NO_OVERLAP) ? new SlotNotAvailableException() : ex;
    }
}
