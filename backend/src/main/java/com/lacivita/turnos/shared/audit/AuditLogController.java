package com.lacivita.turnos.shared.audit;

import com.lacivita.turnos.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auditoría")
@RestController
class AuditLogController {

    private final AuditLogQueries queries;

    AuditLogController(AuditLogQueries queries) {
        this.queries = queries;
    }

    @Operation(
            summary = "Registro de cambios del negocio",
            description = "Solo el dueño. Incluye los accesos de soporte, con su motivo.")
    @GetMapping("/api/businesses/{businessId}/audit-log")
    PageResponse<AuditLogQueries.AuditEntryView> list(
            @PathVariable UUID businessId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, 100));
        return PageResponse.of(queries.forBusiness(businessId, pageable), entry -> entry);
    }
}
