package com.lacivita.turnos.shared.security;

import java.util.UUID;

/**
 * Decide si un ADMIN puede entrar a los datos de un negocio del que no es miembro, y deja registro.
 * Separado del evaluador para poder probar las reglas de permisos sin HTTP ni base de datos.
 */
interface SupportAccess {

    /** {@code true} si el acceso está justificado (y quedó registrado). */
    boolean grant(AuthenticatedUser admin, UUID businessId);
}
