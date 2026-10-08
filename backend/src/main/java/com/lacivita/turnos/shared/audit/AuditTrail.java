package com.lacivita.turnos.shared.audit;

import com.lacivita.turnos.shared.domain.Ids;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/** Escribe el registro de auditoría ({@code audit_log}). Solo agrega filas: nunca modifica ni borra. */
@Component
public class AuditTrail {

    private static final String INSERT = """
            INSERT INTO audit_log (id, business_id, actor_user_id, action, entity_type, entity_id,
                                   old_value, new_value, reason, occurred_at)
            VALUES (:id, :businessId, :actor, :action, :entityType, :entityId,
                    CAST(:oldValue AS JSONB), CAST(:newValue AS JSONB), :reason, :occurredAt)
            """;

    private final JdbcClient jdbc;
    private final JsonMapper json;
    private final Clock clock;

    AuditTrail(JdbcClient jdbc, JsonMapper json, Clock clock) {
        this.jdbc = jdbc;
        this.json = json;
        this.clock = clock;
    }

    /** Se ejecuta dentro de la transacción del cambio, de forma sincrónica. */
    @EventListener
    void on(AuditableEvent event) {
        insert(
                event.auditBusinessId().orElse(null),
                event.auditAction(),
                event.auditEntityType(),
                event.auditEntityId(),
                event.auditBefore(),
                event.auditAfter(),
                null);
    }

    /**
     * Registra que un ADMIN accedió a datos de un negocio. Usa su propia transacción: el acceso queda
     * registrado aunque la operación posterior falle o sea de solo lectura.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordSupportAccess(UUID businessId, String reason) {
        insert(
                businessId,
                "support.access",
                "Business",
                businessId.toString(),
                Optional.empty(),
                Optional.empty(),
                reason);
    }

    private void insert(
            UUID businessId,
            String action,
            String entityType,
            String entityId,
            Optional<Object> before,
            Optional<Object> after,
            String reason) {
        jdbc.sql(INSERT)
                .param("id", Ids.newId())
                .param("businessId", businessId)
                .param("actor", currentActor())
                .param("action", action)
                .param("entityType", entityType)
                .param("entityId", entityId)
                .param("oldValue", before.map(json::writeValueAsString).orElse(null))
                .param("newValue", after.map(json::writeValueAsString).orElse(null))
                .param("reason", reason)
                .param("occurredAt", Timestamp.from(clock.instant()))
                .update();
    }

    private static UUID currentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.id();
        }
        return null;
    }
}
