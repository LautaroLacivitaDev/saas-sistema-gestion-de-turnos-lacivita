package com.lacivita.turnos.shared.security;

/**
 * Rol de una persona dentro de un negocio, de menor a mayor. Cada rol incluye los permisos de los
 * roles inferiores.
 */
public enum BusinessRole {
    BARBER,
    MANAGER,
    OWNER;

    /** {@code true} si este rol alcanza para una acción que exige {@code required}. */
    public boolean includes(BusinessRole required) {
        return this.compareTo(required) >= 0;
    }
}
