package com.lacivita.turnos.catalog.domain;

import com.lacivita.turnos.shared.domain.RuleViolationException;

/** La acción no corresponde al estado actual del servicio (por ejemplo, aprobar uno que ya está activo). */
public class ServiceStatusException extends RuleViolationException {

    private ServiceStatusException(String code, String message) {
        super(code, message);
    }

    static ServiceStatusException notProposed() {
        return new ServiceStatusException("service_not_proposed", "Ese servicio no está esperando aprobación.");
    }

    static ServiceStatusException notActive() {
        return new ServiceStatusException("service_not_active", "Ese servicio no está activo.");
    }

    static ServiceStatusException notInactive() {
        return new ServiceStatusException("service_not_inactive", "Ese servicio ya está activo.");
    }

    static ServiceStatusException rejected() {
        return new ServiceStatusException("service_rejected", "Ese servicio fue rechazado y no se puede editar.");
    }

    static ServiceStatusException notOfferable() {
        return new ServiceStatusException(
                "service_not_offerable", "Solo se pueden ofrecer o combinar servicios activos del catálogo.");
    }
}
