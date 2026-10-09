package com.lacivita.turnos.booking.domain;

import com.lacivita.turnos.shared.domain.NotFoundException;

public class CustomerNotFoundException extends NotFoundException {

    public CustomerNotFoundException() {
        super("customer_not_found", "No encontramos ese cliente.");
    }
}
