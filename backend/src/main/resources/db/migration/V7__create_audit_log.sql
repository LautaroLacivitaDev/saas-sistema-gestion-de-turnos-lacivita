-- Registro de cambios sensibles y de accesos de soporte. Solo se agregan filas: nunca se modifican.

CREATE TABLE audit_log (
    id            UUID                     PRIMARY KEY,
    -- Nulo para eventos que no pertenecen a un negocio. La clave foránea se verifica al confirmar:
    -- el auditor escribe antes de que Hibernate inserte la fila del negocio en la misma transacción.
    business_id   UUID                     REFERENCES business (id) ON DELETE CASCADE DEFERRABLE INITIALLY DEFERRED,
    -- Sin clave foránea: el registro se conserva aunque la cuenta deje de existir.
    actor_user_id UUID,
    action        VARCHAR(60)              NOT NULL,
    entity_type   VARCHAR(60)              NOT NULL,
    entity_id     VARCHAR(60)              NOT NULL,
    old_value     JSONB,
    new_value     JSONB,
    reason        VARCHAR(300),
    occurred_at   TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX audit_log_business_idx ON audit_log (business_id, occurred_at DESC);
