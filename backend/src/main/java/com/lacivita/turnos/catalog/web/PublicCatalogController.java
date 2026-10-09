package com.lacivita.turnos.catalog.web;

import com.lacivita.turnos.catalog.application.CatalogViews.ProfessionalView;
import com.lacivita.turnos.catalog.application.PublicCatalog;
import com.lacivita.turnos.catalog.application.PublicTeam;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Catálogo y profesionales de la página pública de reservas. No exige sesión. */
@Tag(name = "Página pública")
@RestController
class PublicCatalogController {

    static final String PATH = "/api/public/businesses/{slug}/catalog";
    static final String PROFESSIONALS = "/api/public/businesses/{slug}/professionals";

    private final PublicCatalog catalog;
    private final PublicTeam team;

    PublicCatalogController(PublicCatalog catalog, PublicTeam team) {
        this.catalog = catalog;
        this.team = team;
    }

    @Operation(
            summary = "Catálogo público de un negocio",
            description = "Servicios y combos que se pueden reservar, con el precio y la duración de cada"
                    + " profesional y el precio más bajo (desde).")
    @GetMapping(PATH)
    CatalogResponses.PublicCatalog bySlug(@PathVariable String slug) {
        return CatalogResponses.PublicCatalog.from(catalog.bySlug(slug));
    }

    @Operation(
            summary = "Profesionales de un negocio",
            description = "Los que hacen al menos un servicio, con su foto, descripción, especialidades, sucursales"
                    + " (vacío: todas) y precios.")
    @GetMapping(PROFESSIONALS)
    List<ProfessionalView> professionals(@PathVariable String slug) {
        return team.bySlug(slug);
    }

    @Operation(summary = "Perfil público de un profesional")
    @GetMapping(PROFESSIONALS + "/{barberId}")
    ProfessionalView professional(@PathVariable String slug, @PathVariable UUID barberId) {
        return team.one(slug, barberId);
    }
}
