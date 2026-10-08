package com.lacivita.turnos.business;

import com.lacivita.turnos.shared.audit.AuditableEvent;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Se dio de alta un negocio. Se publica dentro de la transacción del alta: el módulo de usuarios lo
 * escucha de forma sincrónica para crear la membresía de dueño, así el negocio nunca existe sin dueño.
 */
public record BusinessRegistered(UUID businessId, UUID ownerUserId, String name, String slug)
        implements AuditableEvent {

    @Override
    public Optional<UUID> auditBusinessId() {
        return Optional.of(businessId);
    }

    @Override
    public String auditAction() {
        return "business.registered";
    }

    @Override
    public String auditEntityType() {
        return "Business";
    }

    @Override
    public String auditEntityId() {
        return businessId.toString();
    }

    @Override
    public Optional<Object> auditAfter() {
        return Optional.of(Map.of("name", name, "slug", slug, "owner", ownerUserId));
    }
}
