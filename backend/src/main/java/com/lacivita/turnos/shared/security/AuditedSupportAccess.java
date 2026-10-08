package com.lacivita.turnos.shared.security;

import com.lacivita.turnos.shared.audit.AuditTrail;
import com.lacivita.turnos.shared.tenancy.TenantContext;
import java.util.UUID;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Acceso de soporte: un ADMIN puede entrar a un negocio solo si explica por qué, en el encabezado
 * {@value #REASON_HEADER}. Cada acceso queda en el registro de auditoría del negocio, que el dueño puede
 * consultar.
 */
class AuditedSupportAccess implements SupportAccess {

    static final String REASON_HEADER = "X-Support-Reason";
    private static final int MAX_REASON_LENGTH = 300;
    private static final String RECORDED_ATTRIBUTE = AuditedSupportAccess.class.getName() + ".recorded.";

    private final AuditTrail auditTrail;

    AuditedSupportAccess(AuditTrail auditTrail) {
        this.auditTrail = auditTrail;
    }

    @Override
    public boolean grant(AuthenticatedUser admin, UUID businessId) {
        if (!admin.isAdmin()) {
            return false;
        }
        var request = RequestContextHolder.getRequestAttributes();
        if (!(request instanceof ServletRequestAttributes servlet)) {
            return false;
        }
        String reason = servlet.getRequest().getHeader(REASON_HEADER);
        if (reason == null || reason.isBlank()) {
            return false;
        }
        recordOncePerRequest(request, businessId, truncate(reason.strip()));
        return true;
    }

    /** Una misma solicitud puede chequear permisos varias veces; se registra un solo acceso. */
    private void recordOncePerRequest(RequestAttributes request, UUID businessId, String reason) {
        String attribute = RECORDED_ATTRIBUTE + businessId;
        if (request.getAttribute(attribute, RequestAttributes.SCOPE_REQUEST) != null) {
            return;
        }
        TenantContext.callAsSystem("registro de acceso de soporte", () -> {
            auditTrail.recordSupportAccess(businessId, reason);
            return null;
        });
        request.setAttribute(attribute, Boolean.TRUE, RequestAttributes.SCOPE_REQUEST);
    }

    private static String truncate(String reason) {
        return reason.length() <= MAX_REASON_LENGTH ? reason : reason.substring(0, MAX_REASON_LENGTH);
    }
}
