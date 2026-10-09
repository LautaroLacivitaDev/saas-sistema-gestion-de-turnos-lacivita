-- Links del cliente para gestionar su turno. Cada email lleva su propio link (la confirmación, los
-- recordatorios, los avisos de cambios), así que un turno puede tener varios. Solo se guarda el SHA-256:
-- el link en claro existe únicamente en el email.

CREATE TABLE appointment_link (
    token_hash     VARCHAR(64)              PRIMARY KEY,
    business_id    UUID                     NOT NULL REFERENCES business (id) ON DELETE CASCADE,
    appointment_id UUID                     NOT NULL REFERENCES appointment (id) ON DELETE CASCADE,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX appointment_link_appointment_idx ON appointment_link (appointment_id);

-- Los links que ya se mandaron siguen funcionando.
INSERT INTO appointment_link (token_hash, business_id, appointment_id, created_at)
SELECT manage_token_hash, business_id, id, updated_at
FROM appointment
WHERE manage_token_hash IS NOT NULL;

DROP INDEX appointment_manage_token_uk;
ALTER TABLE appointment DROP COLUMN manage_token_hash;

ALTER TABLE appointment_link ENABLE ROW LEVEL SECURITY;
CREATE POLICY appointment_link_access ON appointment_link
    USING (app_rls_bypassed() OR business_id = app_current_business())
    WITH CHECK (app_rls_bypassed() OR business_id = app_current_business());
