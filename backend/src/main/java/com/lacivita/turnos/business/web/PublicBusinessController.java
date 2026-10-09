package com.lacivita.turnos.business.web;

import com.lacivita.turnos.business.application.BusinessFinder;
import com.lacivita.turnos.business.application.BusinessViews.SearchResultsView;
import com.lacivita.turnos.business.application.PublicBusinessPages;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Datos públicos de los negocios: el buscador y la página de reservas de cada uno. No exige sesión. */
@Tag(name = "Página pública")
@RestController
@RequestMapping(PublicBusinessController.BASE)
class PublicBusinessController {

    static final String BASE = "/api/public/businesses";

    private final PublicBusinessPages pages;
    private final BusinessFinder finder;

    PublicBusinessController(PublicBusinessPages pages, BusinessFinder finder) {
        this.pages = pages;
        this.finder = finder;
    }

    @Operation(
            summary = "Busca negocios",
            description = "Por nombre, rubro, barrio o ciudad, tolerando errores de tipeo y tildes. Sin q, lista"
                    + " todos. Páginas de 20 (size hasta 50); hasMore indica si hay otra.")
    @GetMapping
    SearchResultsView search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return finder.search(q, page, size);
    }

    @Operation(
            summary = "Página pública de un negocio",
            description = "Acepta también links viejos: si canonicalSlug difiere del pedido, hay que redirigir.")
    @GetMapping("/{slug}")
    BusinessResponses.PublicPage bySlug(@PathVariable String slug) {
        return BusinessResponses.PublicPage.from(pages.bySlug(slug));
    }
}
