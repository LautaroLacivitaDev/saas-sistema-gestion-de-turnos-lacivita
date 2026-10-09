package com.lacivita.turnos.schedule.application;

import org.springframework.dao.DataIntegrityViolationException;

/** Reconoce qué restricción de la base falló, para responder con un error de negocio claro. */
final class Constraints {

    static final String WORK_SHIFT_NO_OVERLAP = "work_shift_no_overlap";
    static final String HOLIDAY_UNIQUE = "holiday_uk";

    private Constraints() {}

    static boolean violated(DataIntegrityViolationException ex, String constraint) {
        String message = ex.getMostSpecificCause().getMessage();
        return message != null && message.contains(constraint);
    }
}
