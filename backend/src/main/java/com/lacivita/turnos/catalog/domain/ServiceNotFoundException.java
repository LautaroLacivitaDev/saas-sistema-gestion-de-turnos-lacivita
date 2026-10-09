package com.lacivita.turnos.catalog.domain;

import com.lacivita.turnos.shared.domain.NotFoundException;

public class ServiceNotFoundException extends NotFoundException {

    public ServiceNotFoundException() {
        super("service_not_found", "No encontramos ese servicio.");
    }
}
