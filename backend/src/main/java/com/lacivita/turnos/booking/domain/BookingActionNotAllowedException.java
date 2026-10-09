package com.lacivita.turnos.booking.domain;

import com.lacivita.turnos.shared.domain.ForbiddenException;

/** La persona pertenece al negocio, pero el turno es de una sucursal que no le corresponde. */
public class BookingActionNotAllowedException extends ForbiddenException {

    public BookingActionNotAllowedException() {
        super("outside_your_branches", "Solo podés gestionar turnos de tus sucursales.");
    }
}
