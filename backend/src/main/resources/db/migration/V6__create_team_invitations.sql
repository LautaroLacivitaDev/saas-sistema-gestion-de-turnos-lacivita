-- Equipo de cada negocio: sucursales asignadas a cada miembro e invitaciones.

ALTER TABLE membership
    ADD CONSTRAINT membership_business_fk FOREIGN KEY (business_id) REFERENCES business (id) ON DELETE CASCADE;

-- Sucursales en las que trabaja un gerente o barbero. El dueño no tiene filas: opera en todas.
CREATE TABLE membership_branch (
    membership_id UUID NOT NULL REFERENCES membership (id) ON DELETE CASCADE,
    branch_id     UUID NOT NULL REFERENCES branch (id) ON DELETE CASCADE,
    PRIMARY KEY (membership_id, branch_id)
);

CREATE INDEX membership_branch_branch_idx ON membership_branch (branch_id);

CREATE TABLE invitation (
    id          UUID                     PRIMARY KEY,
    business_id UUID                     NOT NULL REFERENCES business (id) ON DELETE CASCADE,
    email       VARCHAR(254)             NOT NULL,
    role        VARCHAR(20)              NOT NULL CHECK (role IN ('MANAGER', 'BARBER')),
    token_hash  VARCHAR(64)              NOT NULL,
    invited_by  UUID                     NOT NULL REFERENCES user_account (id),
    expires_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    accepted_at TIMESTAMP WITH TIME ZONE,
    revoked_at  TIMESTAMP WITH TIME ZONE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    version     BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT invitation_token_hash_uk UNIQUE (token_hash)
);

-- A lo sumo una invitación pendiente por email y negocio.
CREATE UNIQUE INDEX invitation_pending_email_uk
    ON invitation (business_id, email)
    WHERE accepted_at IS NULL AND revoked_at IS NULL;

CREATE TABLE invitation_branch (
    invitation_id UUID NOT NULL REFERENCES invitation (id) ON DELETE CASCADE,
    branch_id     UUID NOT NULL REFERENCES branch (id) ON DELETE CASCADE,
    PRIMARY KEY (invitation_id, branch_id)
);
