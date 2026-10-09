package com.lacivita.turnos.business.domain;

import java.util.List;
import java.util.UUID;

/** Buscador de negocios que aceptan aparecer en las búsquedas. */
public interface BusinessSearch {

    /**
     * Negocios que coinciden con todas las palabras, en su nombre, su rubro o el barrio y la ciudad de alguna
     * sucursal, aunque tengan errores de tipeo o falten tildes. Primero los que mejor coinciden por nombre.
     *
     * @param limit cuántos traer
     * @param offset cuántos saltear (para paginar)
     */
    List<Hit> search(SearchTerms terms, int limit, int offset);

    record Hit(UUID id, String name, String slug, String category, String description) {}
}
