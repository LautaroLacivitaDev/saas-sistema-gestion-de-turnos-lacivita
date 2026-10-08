-- Cuentas de usuario, identidades externas (Google), tokens de un solo uso y membresías.

CREATE TABLE user_account (
    id                UUID                     PRIMARY KEY,
    name              VARCHAR(120)             NOT NULL,
    -- Se guarda normalizado en minúsculas (lo garantiza el objeto de valor Email).
    email             VARCHAR(254)             NOT NULL,
    phone             VARCHAR(30),
    -- Nulo cuando la cuenta solo entra con Google o con link de acceso.
    password_hash     VARCHAR(100),
    email_verified_at TIMESTAMP WITH TIME ZONE,
    platform_role     VARCHAR(20)              NOT NULL CHECK (platform_role IN ('USER', 'ADMIN')),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    version           BIGINT                   NOT NULL DEFAULT 0,
    CONSTRAINT user_account_email_uk UNIQUE (email)
);

CREATE TABLE user_identity (
    id        UUID                     PRIMARY KEY,
    user_id   UUID                     NOT NULL REFERENCES user_account (id) ON DELETE CASCADE,
    provider  VARCHAR(30)              NOT NULL CHECK (provider IN ('GOOGLE')),
    subject   VARCHAR(255)             NOT NULL,
    linked_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT user_identity_provider_subject_uk UNIQUE (provider, subject),
    CONSTRAINT user_identity_user_provider_uk UNIQUE (user_id, provider)
);

-- Tokens de verificación de email y de link de acceso. Solo se guarda el hash SHA-256:
-- quien lea la base no puede usar los tokens.
CREATE TABLE auth_token (
    id         UUID                     PRIMARY KEY,
    user_id    UUID                     NOT NULL REFERENCES user_account (id) ON DELETE CASCADE,
    purpose    VARCHAR(30)              NOT NULL CHECK (purpose IN ('EMAIL_VERIFICATION', 'LOGIN_LINK')),
    token_hash VARCHAR(64)              NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    used_at    TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT auth_token_hash_uk UNIQUE (token_hash)
);

CREATE INDEX auth_token_expires_at_idx ON auth_token (expires_at);

-- Rol de una persona dentro de un negocio. La clave foránea a business se agrega
-- en la migración que crea esa tabla (Hito 3).
CREATE TABLE membership (
    id          UUID                     PRIMARY KEY,
    user_id     UUID                     NOT NULL REFERENCES user_account (id) ON DELETE CASCADE,
    business_id UUID                     NOT NULL,
    role        VARCHAR(20)              NOT NULL CHECK (role IN ('OWNER', 'MANAGER', 'BARBER')),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT membership_user_business_uk UNIQUE (user_id, business_id)
);

CREATE INDEX membership_business_idx ON membership (business_id);
