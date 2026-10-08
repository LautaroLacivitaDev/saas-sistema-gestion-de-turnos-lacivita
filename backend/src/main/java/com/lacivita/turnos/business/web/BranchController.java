package com.lacivita.turnos.business.web;

import com.lacivita.turnos.business.application.BranchManagement;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Sucursales")
@RestController
@RequestMapping("/api/businesses/{businessId}/branches")
class BranchController {

    private final BranchManagement branches;

    BranchController(BranchManagement branches) {
        this.branches = branches;
    }

    @Operation(summary = "Lista las sucursales del negocio")
    @GetMapping
    List<BusinessResponses.Branch> list(@PathVariable UUID businessId) {
        return branches.list(businessId).stream()
                .map(BusinessResponses.Branch::from)
                .toList();
    }

    @Operation(summary = "Crea una sucursal", description = "Solo el dueño.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BusinessResponses.Branch open(@PathVariable UUID businessId, @Valid @RequestBody BusinessRequests.BranchData body) {
        return BusinessResponses.Branch.from(branches.open(businessId, body.details()));
    }

    @Operation(summary = "Cambia los datos de una sucursal", description = "Solo el dueño.")
    @PutMapping("/{branchId}")
    BusinessResponses.Branch update(
            @PathVariable UUID businessId,
            @PathVariable UUID branchId,
            @Valid @RequestBody BusinessRequests.BranchData body) {
        return BusinessResponses.Branch.from(branches.update(businessId, branchId, body.details()));
    }
}
