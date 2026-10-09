package com.lacivita.turnos.booking.domain;

import com.lacivita.turnos.shared.security.BusinessMembership;
import com.lacivita.turnos.shared.security.BusinessRole;
import java.util.UUID;

/**
 * Quién ve y gestiona qué turnos.
 *
 * <ul>
 *   <li>Cada persona del equipo ve y gestiona los turnos de sus sucursales (el dueño, de todas). El
 *       barbero siempre ve su propia agenda.
 *   <li>Los datos de contacto del cliente los ven el dueño, los gerentes y el profesional del turno.
 * </ul>
 */
// DECISIÓN: es el comportamiento por defecto de la especificación. Que el dueño restrinja lo que ve
// cada barbero (solo sus turnos, o el teléfono de clientes ajenos) queda para cuando haya panel.
public final class BookingPolicy {

    private BookingPolicy() {}

    public static void checkCanManage(BusinessMembership actor, UUID branchId) {
        if (!actor.covers(branchId)) {
            throw new BookingActionNotAllowedException();
        }
    }

    public static boolean canSee(BusinessMembership actor, UUID actorId, Appointment appointment) {
        return actor.covers(appointment.getBranchId())
                || appointment.getBarberId().equals(actorId);
    }

    public static boolean canSeeContact(BusinessMembership actor, UUID actorId, Appointment appointment) {
        return actor.role().includes(BusinessRole.MANAGER)
                || appointment.getBarberId().equals(actorId);
    }
}
