package com.lacivita.turnos.booking.domain;

import com.lacivita.turnos.shared.domain.RuleViolationException;

/** El cliente quiere cancelar o reprogramar después del plazo del negocio. */
public class ChangeDeadlinePassedException extends RuleViolationException {

    public ChangeDeadlinePassedException(int noticeHours) {
        super(
                "change_deadline_passed",
                "Los turnos se pueden cancelar o reprogramar hasta " + noticeHours
                        + " horas antes. Comunicate con el local.");
    }
}
