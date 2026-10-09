package com.lacivita.turnos.schedule.domain;

import com.lacivita.turnos.shared.domain.ConflictException;

public class HolidayAlreadyExistsException extends ConflictException {

    public HolidayAlreadyExistsException() {
        super("holiday_exists", "Ya hay un feriado cargado ese día.");
    }
}
