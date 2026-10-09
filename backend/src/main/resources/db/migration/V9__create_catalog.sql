-- Catálogo: servicios del negocio, combos y lo que ofrece cada barbero con su precio y duración.
-- Importes en pesos con dos decimales; duraciones en minutos.

CREATE TABLE service (
    id            UUID                     PRIMARY KEY,
    business_id   UUID                     NOT NULL REFERENCES business (id) ON DELETE CASCADE,
    name          VARCHAR(80)              NOT NULL,
    category      VARCHAR(40)              NOT NULL,
    description   VARCHAR(500),
    base_duration INTEGER                  NOT NULL CHECK (base_duration > 0 AND base_duration % 5 = 0),
    base_price    NUMERIC(10, 2)           NOT NULL CHECK (base_price >= 0),
    -- Rango en el que cada barbero elige su precio. Los dos nulos: sin rango.
    price_min     NUMERIC(10, 2),
    price_max     NUMERIC(10, 2),
    status        VARCHAR(20)              NOT NULL
        CHECK (status IN ('PROPOSED', 'ACTIVE', 'INACTIVE', 'REJECTED')),
    proposed_by   UUID                     REFERENCES user_account (id) ON DELETE SET NULL,
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    version       BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT service_price_range_ck CHECK (
        (price_min IS NULL AND price_max IS NULL)
            OR (price_min IS NOT NULL AND price_max IS NOT NULL AND price_min <= price_max))
);

-- Dos servicios del mismo negocio no comparten nombre (sin distinguir mayúsculas). Las propuestas
-- rechazadas no cuentan: se puede volver a proponer o crear un servicio con ese nombre.
CREATE UNIQUE INDEX service_name_uk ON service (business_id, lower(name)) WHERE status <> 'REJECTED';
CREATE INDEX service_business_status_idx ON service (business_id, status);

CREATE TABLE combo (
    id          UUID                     PRIMARY KEY,
    business_id UUID                     NOT NULL REFERENCES business (id) ON DELETE CASCADE,
    name        VARCHAR(80)              NOT NULL,
    active      BOOLEAN                  NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    version     BIGINT                   NOT NULL DEFAULT 0
);

CREATE INDEX combo_business_idx ON combo (business_id);

CREATE TABLE combo_service (
    combo_id   UUID    NOT NULL REFERENCES combo (id) ON DELETE CASCADE,
    position   INTEGER NOT NULL,
    service_id UUID    NOT NULL REFERENCES service (id),
    PRIMARY KEY (combo_id, position)
);

CREATE TABLE barber_service (
    id                 UUID                     PRIMARY KEY,
    business_id        UUID                     NOT NULL REFERENCES business (id) ON DELETE CASCADE,
    barber_id          UUID                     NOT NULL REFERENCES user_account (id),
    service_id         UUID                     NOT NULL REFERENCES service (id),
    -- Precio y duración propios; nulos si hereda los valores base del servicio.
    price              NUMERIC(10, 2)           CHECK (price >= 0),
    duration           INTEGER                  CHECK (duration > 0 AND duration % 5 = 0),
    active             BOOLEAN                  NOT NULL,
    -- Precio fuera de rango que espera la aprobación de un gerente.
    requested_price    NUMERIC(10, 2)           CHECK (requested_price >= 0),
    price_requested_at TIMESTAMP WITH TIME ZONE,
    created_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    version            BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT barber_service_uk UNIQUE (barber_id, service_id)
);

CREATE INDEX barber_service_service_idx ON barber_service (service_id) WHERE active;
CREATE INDEX barber_service_pending_idx ON barber_service (business_id) WHERE requested_price IS NOT NULL;

-- Aislamiento: todo se lee y se escribe dentro del negocio. El catálogo público también se arma dentro
-- del negocio del link, así que no hace falta lectura abierta.
ALTER TABLE service ENABLE ROW LEVEL SECURITY;
CREATE POLICY service_access ON service
    USING (app_rls_bypassed() OR business_id = app_current_business())
    WITH CHECK (app_rls_bypassed() OR business_id = app_current_business());

ALTER TABLE combo ENABLE ROW LEVEL SECURITY;
CREATE POLICY combo_access ON combo
    USING (app_rls_bypassed() OR business_id = app_current_business())
    WITH CHECK (app_rls_bypassed() OR business_id = app_current_business());

ALTER TABLE combo_service ENABLE ROW LEVEL SECURITY;
CREATE POLICY combo_service_access ON combo_service
    USING (EXISTS (SELECT 1 FROM combo c WHERE c.id = combo_service.combo_id))
    WITH CHECK (EXISTS (SELECT 1 FROM combo c WHERE c.id = combo_service.combo_id));

ALTER TABLE barber_service ENABLE ROW LEVEL SECURITY;
CREATE POLICY barber_service_access ON barber_service
    USING (app_rls_bypassed() OR business_id = app_current_business())
    WITH CHECK (app_rls_bypassed() OR business_id = app_current_business());
