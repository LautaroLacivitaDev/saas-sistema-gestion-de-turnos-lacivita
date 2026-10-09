package com.lacivita.turnos.catalog.domain;

/** Ciclo de vida de un servicio del catálogo. */
public enum ServiceStatus {
    /** Lo propuso un barbero y espera la aprobación de un gerente. No se puede reservar. */
    PROPOSED,
    /** En el catálogo: los barberos lo pueden ofrecer y los clientes, reservar. */
    ACTIVE,
    /** Retirado del catálogo. Se conserva para los turnos y reportes históricos. */
    INACTIVE,
    /** Propuesta rechazada. */
    REJECTED
}
