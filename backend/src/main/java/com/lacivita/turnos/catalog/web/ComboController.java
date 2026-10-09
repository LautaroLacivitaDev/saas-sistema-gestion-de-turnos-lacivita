package com.lacivita.turnos.catalog.web;

import com.lacivita.turnos.catalog.application.Combos;
import com.lacivita.turnos.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Catálogo: combos")
@RestController
@RequestMapping("/api/businesses/{businessId}/combos")
class ComboController {

    private final Combos combos;

    ComboController(Combos combos) {
        this.combos = combos;
    }

    @Operation(summary = "Lista los combos", description = "Todo el equipo.")
    @GetMapping
    PageResponse<CatalogResponses.Combo> list(
            @PathVariable UUID businessId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = CatalogPages.DEFAULT_SIZE) int size) {
        return PageResponse.of(
                combos.list(businessId, CatalogPages.of(page, size, Sort.by("name"))), CatalogResponses.Combo::from);
    }

    @Operation(
            summary = "Arma un combo",
            description = "Gerentes y dueño. De 2 a 5 servicios activos que hace un mismo profesional; el precio y"
                    + " la duración son la suma de los de ese profesional.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CatalogResponses.Combo create(@PathVariable UUID businessId, @Valid @RequestBody CatalogRequests.ComboData body) {
        return CatalogResponses.Combo.from(combos.create(businessId, body.name(), body.serviceIds()));
    }

    @Operation(summary = "Cambia un combo", description = "Gerentes y dueño.")
    @PutMapping("/{comboId}")
    CatalogResponses.Combo update(
            @PathVariable UUID businessId,
            @PathVariable UUID comboId,
            @Valid @RequestBody CatalogRequests.ComboData body) {
        return CatalogResponses.Combo.from(
                combos.update(businessId, comboId, body.name(), body.serviceIds(), body.isActive()));
    }
}
