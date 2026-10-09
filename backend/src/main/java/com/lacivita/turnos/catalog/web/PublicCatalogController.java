package com.lacivita.turnos.catalog.web;

import com.lacivita.turnos.catalog.application.PublicCatalog;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Catálogo de la página pública de reservas. No exige sesión. */
@Tag(name = "Página pública")
@RestController
class PublicCatalogController {

    static final String PATH = "/api/public/businesses/{slug}/catalog";

    private final PublicCatalog catalog;

    PublicCatalogController(PublicCatalog catalog) {
        this.catalog = catalog;
    }

    @Operation(
            summary = "Catálogo público de un negocio",
            description = "Servicios y combos que se pueden reservar, con el precio y la duración de cada"
                    + " profesional y el precio más bajo (desde).")
    @GetMapping(PATH)
    CatalogResponses.PublicCatalog bySlug(@PathVariable String slug) {
        return CatalogResponses.PublicCatalog.from(catalog.bySlug(slug));
    }
}
