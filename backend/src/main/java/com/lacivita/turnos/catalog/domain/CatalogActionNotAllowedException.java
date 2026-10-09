package com.lacivita.turnos.catalog.domain;

import com.lacivita.turnos.shared.domain.ForbiddenException;

/** La persona pertenece al negocio, pero su rol no le permite esta acción sobre el catálogo. */
public class CatalogActionNotAllowedException extends ForbiddenException {

    private CatalogActionNotAllowedException(String code, String message) {
        super(code, message);
    }

    static CatalogActionNotAllowedException onlyOwnOfferings() {
        return new CatalogActionNotAllowedException(
                "only_own_offerings", "Solo podés cambiar tus propios servicios y precios.");
    }

    static CatalogActionNotAllowedException onlyOwnerManagesManagers() {
        return new CatalogActionNotAllowedException(
                "only_owner_manages_managers", "Solo el dueño puede cambiar los servicios y precios de un gerente.");
    }

    static CatalogActionNotAllowedException outsideActorBranches() {
        return new CatalogActionNotAllowedException(
                "outside_your_branches", "Solo podés gestionar personas de tus sucursales.");
    }
}
