-- Negocios, registro de slugs y sucursales.

CREATE TABLE business (
    id          UUID                     PRIMARY KEY,
    name        VARCHAR(80)              NOT NULL,
    -- Slug actual. El historial completo (para redirigir links viejos) está en business_slug.
    slug        VARCHAR(50)              NOT NULL,
    category    VARCHAR(30)              NOT NULL
        CHECK (category IN ('BARBERSHOP', 'BEAUTY_SALON', 'HAIR_SALON', 'NAIL_SALON')),
    description VARCHAR(1000),
    searchable  BOOLEAN                  NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    version     BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT business_slug_uk UNIQUE (slug)
);

-- Todos los slugs que usó cada negocio. La clave primaria garantiza que un slug, una vez usado,
-- no lo pueda tomar otro negocio: los links compartidos siguen llevando al negocio correcto.
CREATE TABLE business_slug (
    slug        VARCHAR(50)              PRIMARY KEY,
    business_id UUID                     NOT NULL REFERENCES business (id) ON DELETE CASCADE,
    claimed_at  TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX business_slug_business_idx ON business_slug (business_id);

CREATE TABLE branch (
    id           UUID                     PRIMARY KEY,
    business_id  UUID                     NOT NULL REFERENCES business (id) ON DELETE CASCADE,
    name         VARCHAR(80)              NOT NULL,
    street       VARCHAR(150)             NOT NULL,
    neighborhood VARCHAR(80),
    city         VARCHAR(80)              NOT NULL,
    latitude     NUMERIC(9, 6),
    longitude    NUMERIC(9, 6),
    phone        VARCHAR(20),
    time_zone    VARCHAR(60)              NOT NULL,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    version      BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT branch_coordinates_ck CHECK ((latitude IS NULL) = (longitude IS NULL))
);

CREATE INDEX branch_business_idx ON branch (business_id);
