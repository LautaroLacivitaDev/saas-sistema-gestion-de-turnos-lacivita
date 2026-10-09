package com.lacivita.turnos.booking.domain;

import com.lacivita.turnos.shared.domain.RuleViolationException;

/** El cambio de estado no corresponde al estado actual del turno. */
public class AppointmentStatusException extends RuleViolationException {

    private AppointmentStatusException(String code, String message) {
        super(code, message);
    }

    static AppointmentStatusException notAllowedFrom(AppointmentStatus current) {
        return new AppointmentStatusException(
                "invalid_status_change", "Ese cambio no se puede hacer en un turno " + label(current) + ".");
    }

    static AppointmentStatusException notStartedYet() {
        return new AppointmentStatusException("appointment_not_started", "Todavía no llegó la hora del turno.");
    }

    private static String label(AppointmentStatus status) {
        return switch (status) {
            case HOLD -> "sin confirmar";
            case PENDING -> "a confirmar";
            case CONFIRMED -> "confirmado";
            case IN_PROGRESS -> "en curso";
            case COMPLETED -> "terminado";
            case CANCELLED -> "cancelado";
            case NO_SHOW -> "en el que el cliente no vino";
        };
    }
}
