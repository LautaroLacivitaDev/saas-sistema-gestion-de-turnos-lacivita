package com.lacivita.turnos.schedule.domain;

import com.lacivita.turnos.shared.domain.NotFoundException;

public class HolidayNotFoundException extends NotFoundException {

    public HolidayNotFoundException() {
        super("holiday_not_found", "No encontramos ese feriado.");
    }
}
