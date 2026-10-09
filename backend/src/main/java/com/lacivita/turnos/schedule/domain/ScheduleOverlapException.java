package com.lacivita.turnos.schedule.domain;

import com.lacivita.turnos.shared.domain.ConflictException;

/** Lo detecta la restricción de exclusión de la base, también entre dos guardados simultáneos. */
public class ScheduleOverlapException extends ConflictException {

    public ScheduleOverlapException() {
        super("schedule_overlap", "Ese horario se superpone con otro del mismo profesional, en esta u otra sucursal.");
    }
}
