package com.lacivita.turnos.catalog.domain;

import com.lacivita.turnos.shared.security.BusinessMembership;
import com.lacivita.turnos.shared.security.BusinessRole;
import java.util.UUID;

/**
 * Quién puede cambiar los servicios y precios de quién.
 *
 * <ul>
 *   <li>Cada persona del equipo gestiona los propios.
 *   <li>El dueño gestiona los de cualquiera.
 *   <li>El gerente gestiona los de los barberos de sus sucursales.
 *   <li>Gerentes y dueño aprueban precios: sus cambios se aplican aunque estén fuera del rango.
 * </ul>
 *
 * <p>Que la persona pertenezca al negocio lo verifica antes {@code @PreAuthorize}; estas reglas deciden
 * sobre la acción concreta.
 */
public final class CatalogPolicy {

    private CatalogPolicy() {}

    public static void checkCanManageOfferingsOf(
            UUID actorId, BusinessMembership actor, UUID barberId, BusinessMembership barber) {
        if (actorId.equals(barberId)) {
            return;
        }
        switch (actor.role()) {
            case OWNER -> {}
            case MANAGER -> {
                if (barber.role() != BusinessRole.BARBER) {
                    throw CatalogActionNotAllowedException.onlyOwnerManagesManagers();
                }
                if (!barber.branchIds().stream().allMatch(actor::covers)) {
                    throw CatalogActionNotAllowedException.outsideActorBranches();
                }
            }
            case BARBER -> throw CatalogActionNotAllowedException.onlyOwnOfferings();
        }
    }

    /** {@code true} si los cambios de precio de esta persona no necesitan aprobación. */
    public static boolean approvesPrices(BusinessMembership actor) {
        return actor.role().includes(BusinessRole.MANAGER);
    }
}
