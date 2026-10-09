package com.lacivita.turnos.catalog.application;

import com.lacivita.turnos.catalog.domain.Service;
import com.lacivita.turnos.catalog.domain.ServiceNameTakenException;
import com.lacivita.turnos.catalog.domain.ServiceRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;

/**
 * Escribe los servicios enseguida, dentro del caso de uso, para que un nombre repetido (lo decide el
 * índice único de la base, también entre dos pedidos simultáneos) responda con un error claro.
 */
@Component
class ServiceNames {

    private static final String NAME_CONSTRAINT = "service_name_uk";

    private final ServiceRepository services;

    ServiceNames(ServiceRepository services) {
        this.services = services;
    }

    Service saveNew(Service service) {
        try {
            return services.saveAndFlush(service);
        } catch (DataIntegrityViolationException ex) {
            throw translated(ex);
        }
    }

    /** Escribe los cambios de un servicio ya cargado (por ejemplo, un nombre nuevo o una reactivación). */
    void flushChanges() {
        try {
            services.flush();
        } catch (DataIntegrityViolationException ex) {
            throw translated(ex);
        }
    }

    private static RuntimeException translated(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        return message != null && message.contains(NAME_CONSTRAINT) ? new ServiceNameTakenException() : ex;
    }
}
