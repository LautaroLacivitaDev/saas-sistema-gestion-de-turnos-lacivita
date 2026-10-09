-- Perfil público de cada profesional en un negocio: foto, descripción y especialidades.

CREATE TABLE barber_profile (
    id          UUID                     PRIMARY KEY,
    business_id UUID                     NOT NULL REFERENCES business (id) ON DELETE CASCADE,
    barber_id   UUID                     NOT NULL REFERENCES user_account (id) ON DELETE CASCADE,
    bio         VARCHAR(500),
    specialties VARCHAR(40)[]            NOT NULL CHECK (cardinality(specialties) <= 6),
    -- Dirección https de la foto. Subir fotos a un almacenamiento propio queda para más adelante.
    photo_url   VARCHAR(500),
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    version     BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT barber_profile_uk UNIQUE (business_id, barber_id)
);

ALTER TABLE barber_profile ENABLE ROW LEVEL SECURITY;
CREATE POLICY barber_profile_access ON barber_profile
    USING (app_rls_bypassed() OR business_id = app_current_business())
    WITH CHECK (app_rls_bypassed() OR business_id = app_current_business());
