package com.lacivita.turnos.shared.tenancy;

import java.util.UUID;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;

/**
 * Primera barrera de aislamiento: Hibernate agrega automáticamente {@code business_id = :negocio} a
 * toda consulta sobre entidades con {@code @TenantId}, y completa ese campo al insertar.
 *
 * <p>Sin negocio en el contexto (operaciones de sistema, datos públicos o tablas que no son de un
 * negocio) se usa un tenant "raíz" que no filtra. En ese caso la segunda barrera, Row Level Security,
 * sigue aplicando.
 */
class BusinessTenantResolver implements CurrentTenantIdentifierResolver<UUID> {

    static final UUID ROOT = new UUID(0L, 0L);

    @Override
    public UUID resolveCurrentTenantIdentifier() {
        return TenantContext.currentBusiness().orElse(ROOT);
    }

    @Override
    public boolean isRoot(UUID tenantId) {
        return ROOT.equals(tenantId);
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}
