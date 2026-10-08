package com.lacivita.turnos.shared.audit;

import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** Consulta del registro de auditoría de un negocio. Solo el dueño, que así ve también los accesos de soporte. */
@Service
public class AuditLogQueries {

    private static final String PAGE = """
            SELECT id, actor_user_id, action, entity_type, entity_id, old_value, new_value, reason, occurred_at
            FROM audit_log
            WHERE business_id = :businessId
            ORDER BY occurred_at DESC, id DESC
            LIMIT :limit OFFSET :offset
            """;

    private final JdbcClient jdbc;
    private final JsonMapper json;

    AuditLogQueries(JdbcClient jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    public record AuditEntryView(
            UUID id,
            UUID actorUserId,
            String action,
            String entityType,
            String entityId,
            JsonNode before,
            JsonNode after,
            String reason,
            Instant occurredAt) {}

    @BusinessScoped
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#businessId, 'Business', 'OWNER')")
    public Page<AuditEntryView> forBusiness(@BusinessId UUID businessId, Pageable pageable) {
        var entries = jdbc.sql(PAGE)
                .param("businessId", businessId)
                .param("limit", pageable.getPageSize())
                .param("offset", pageable.getOffset())
                .query(this::toView)
                .list();
        long total = jdbc.sql("SELECT count(*) FROM audit_log WHERE business_id = :businessId")
                .param("businessId", businessId)
                .query(Long.class)
                .single();
        return new PageImpl<>(entries, pageable, total);
    }

    private AuditEntryView toView(ResultSet row, int rowNumber) throws SQLException {
        return new AuditEntryView(
                row.getObject("id", UUID.class),
                row.getObject("actor_user_id", UUID.class),
                row.getString("action"),
                row.getString("entity_type"),
                row.getString("entity_id"),
                readJson(row.getString("old_value")),
                readJson(row.getString("new_value")),
                row.getString("reason"),
                row.getTimestamp("occurred_at").toInstant());
    }

    private JsonNode readJson(String value) {
        return value == null ? null : json.readTree(value);
    }
}
