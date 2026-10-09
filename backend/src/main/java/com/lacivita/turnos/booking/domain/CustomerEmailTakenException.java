package com.lacivita.turnos.booking.domain;

import com.lacivita.turnos.shared.domain.ConflictException;

/** Otro cliente del negocio ya tiene ese email. */
public class CustomerEmailTakenException extends ConflictException {

    public CustomerEmailTakenException() {
        super("customer_email_taken", "Otro cliente ya tiene ese email.");
    }
}
