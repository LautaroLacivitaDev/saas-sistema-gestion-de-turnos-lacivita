package com.lacivita.turnos.business.web;

import com.lacivita.turnos.business.application.PublicBusinessPages;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Datos públicos de un negocio para su página de reservas. No exige sesión. */
@Tag(name = "Página pública")
@RestController
@RequestMapping(PublicBusinessController.BASE)
class PublicBusinessController {

    static final String BASE = "/api/public/businesses";

    private final PublicBusinessPages pages;

    PublicBusinessController(PublicBusinessPages pages) {
        this.pages = pages;
    }

    @Operation(
            summary = "Página pública de un negocio",
            description = "Acepta también links viejos: si canonicalSlug difiere del pedido, hay que redirigir.")
    @GetMapping("/{slug}")
    BusinessResponses.PublicPage bySlug(@PathVariable String slug) {
        return BusinessResponses.PublicPage.from(pages.bySlug(slug));
    }
}
