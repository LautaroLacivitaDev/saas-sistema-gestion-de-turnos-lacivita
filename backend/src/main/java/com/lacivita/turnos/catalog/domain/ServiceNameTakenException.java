package com.lacivita.turnos.catalog.domain;

import com.lacivita.turnos.shared.domain.ConflictException;

/** Ya hay un servicio con ese nombre en el catálogo (sin contar los rechazados). */
public class ServiceNameTakenException extends ConflictException {

    public ServiceNameTakenException() {
        super("service_name_taken", "Ya hay un servicio con ese nombre en el catálogo.");
    }
}
