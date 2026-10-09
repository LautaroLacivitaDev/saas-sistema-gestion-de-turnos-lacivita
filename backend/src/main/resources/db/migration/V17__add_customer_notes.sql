-- Base de clientes del panel: notas y preferencias que carga el equipo, y búsqueda por nombre, email o
-- teléfono.

ALTER TABLE customer ADD COLUMN notes VARCHAR(2000);
ALTER TABLE customer ADD COLUMN preferences VARCHAR(500);

-- Búsqueda sin tildes y por partes ("gonz" encuentra "González"): LIKE sobre el texto normalizado, con
-- índice de trigramas.
CREATE INDEX customer_search_trgm_idx ON customer
    USING gin (app_search_text(name || ' ' || coalesce(email, '') || ' ' || coalesce(phone, '')) gin_trgm_ops);

-- Historial de cada cliente, del turno más nuevo al más viejo.
CREATE INDEX appointment_customer_starts_idx ON appointment (customer_id, starts_at DESC);
