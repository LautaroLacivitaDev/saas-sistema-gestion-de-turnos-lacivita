-- Reservas: clientes de cada negocio, turnos con el precio copiado al reservar, la verificación de
-- quien reserva como invitado y las políticas de reserva del negocio.

CREATE TABLE customer (
    id          UUID                     PRIMARY KEY,
    business_id UUID                     NOT NULL REFERENCES business (id) ON DELETE CASCADE,
    user_id     UUID                     REFERENCES user_account (id) ON DELETE SET NULL,
    name        VARCHAR(120)             NOT NULL,
    -- Normalizado en minúsculas (lo garantiza el objeto de valor Email).
    email       VARCHAR(254),
    phone       VARCHAR(20),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CHECK (email IS NOT NULL OR phone IS NOT NULL)
);

-- Un cliente por email y por cuenta dentro de cada negocio.
CREATE UNIQUE INDEX customer_email_uk ON customer (business_id, email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX customer_user_uk ON customer (business_id, user_id) WHERE user_id IS NOT NULL;
CREATE INDEX customer_phone_idx ON customer (business_id, phone) WHERE phone IS NOT NULL;

CREATE TABLE appointment (
    id                UUID                     PRIMARY KEY,
    business_id       UUID                     NOT NULL REFERENCES business (id) ON DELETE CASCADE,
    branch_id         UUID                     NOT NULL REFERENCES branch (id),
    barber_id         UUID                     NOT NULL REFERENCES user_account (id),
    customer_id       UUID                     REFERENCES customer (id),
    combo_id          UUID                     REFERENCES combo (id),
    status            VARCHAR(20)              NOT NULL
        CHECK (status IN ('HOLD', 'PENDING', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    source            VARCHAR(20)              NOT NULL CHECK (source IN ('WEB', 'COUNTER')),
    starts_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    hold_expires_at   TIMESTAMP WITH TIME ZONE,
    total_price       NUMERIC(10, 2)           NOT NULL CHECK (total_price >= 0),
    manage_token_hash VARCHAR(64),
    created_by        UUID                     REFERENCES user_account (id),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    version           BIGINT                   NOT NULL DEFAULT 0,
    CHECK (ends_at > starts_at),
    -- Solo un HOLD vence; los demás estados necesitan saber quién es el cliente.
    CHECK ((status = 'HOLD') = (hold_expires_at IS NOT NULL)),
    CHECK (status = 'HOLD' OR customer_id IS NOT NULL),
    -- Prevención de doble reserva en la base: dos turnos activos del mismo profesional no se superponen,
    -- aunque dos personas reserven el mismo horario al mismo tiempo.
    CONSTRAINT appointment_no_overlap EXCLUDE USING gist (
        business_id WITH =,
        barber_id WITH =,
        tstzrange(starts_at, ends_at, '[)') WITH &&)
        WHERE (status NOT IN ('CANCELLED', 'NO_SHOW'))
);

CREATE UNIQUE INDEX appointment_manage_token_uk ON appointment (manage_token_hash)
    WHERE manage_token_hash IS NOT NULL;
CREATE INDEX appointment_business_starts_idx ON appointment (business_id, starts_at);
CREATE INDEX appointment_barber_starts_idx ON appointment (barber_id, starts_at);
CREATE INDEX appointment_customer_idx ON appointment (customer_id);

-- Servicios del turno con el precio y la duración vigentes al reservar.
CREATE TABLE appointment_line (
    appointment_id   UUID           NOT NULL REFERENCES appointment (id) ON DELETE CASCADE,
    position         INTEGER        NOT NULL,
    service_id       UUID           NOT NULL REFERENCES service (id),
    service_name     VARCHAR(80)    NOT NULL,
    price            NUMERIC(10, 2) NOT NULL CHECK (price >= 0),
    duration_minutes INTEGER        NOT NULL CHECK (duration_minutes > 0),
    PRIMARY KEY (appointment_id, position)
);

-- Datos y código de quien reserva como invitado, mientras confirma su turno.
CREATE TABLE guest_check (
    appointment_id UUID                     PRIMARY KEY REFERENCES appointment (id) ON DELETE CASCADE,
    business_id    UUID                     NOT NULL REFERENCES business (id) ON DELETE CASCADE,
    name           VARCHAR(120),
    email          VARCHAR(254),
    phone          VARCHAR(20),
    code_hash      VARCHAR(64),
    expires_at     TIMESTAMP WITH TIME ZONE,
    attempts       INTEGER                  NOT NULL DEFAULT 0
);

CREATE TABLE booking_settings (
    business_id          UUID                     PRIMARY KEY REFERENCES business (id) ON DELETE CASCADE,
    notice_hours         INTEGER                  NOT NULL CHECK (notice_hours BETWEEN 0 AND 168),
    updated_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    version              BIGINT                   NOT NULL DEFAULT 0
);

-- Aislamiento: todo se lee y se escribe dentro del negocio. Las reservas públicas y los links del cliente
-- también operan dentro del negocio del turno.
ALTER TABLE customer ENABLE ROW LEVEL SECURITY;
CREATE POLICY customer_access ON customer
    USING (app_rls_bypassed() OR business_id = app_current_business())
    WITH CHECK (app_rls_bypassed() OR business_id = app_current_business());

ALTER TABLE appointment ENABLE ROW LEVEL SECURITY;
CREATE POLICY appointment_access ON appointment
    USING (app_rls_bypassed() OR business_id = app_current_business())
    WITH CHECK (app_rls_bypassed() OR business_id = app_current_business());

ALTER TABLE appointment_line ENABLE ROW LEVEL SECURITY;
CREATE POLICY appointment_line_access ON appointment_line
    USING (EXISTS (SELECT 1 FROM appointment a WHERE a.id = appointment_line.appointment_id))
    WITH CHECK (EXISTS (SELECT 1 FROM appointment a WHERE a.id = appointment_line.appointment_id));

ALTER TABLE guest_check ENABLE ROW LEVEL SECURITY;
CREATE POLICY guest_check_access ON guest_check
    USING (app_rls_bypassed() OR business_id = app_current_business())
    WITH CHECK (app_rls_bypassed() OR business_id = app_current_business());

ALTER TABLE booking_settings ENABLE ROW LEVEL SECURITY;
CREATE POLICY booking_settings_access ON booking_settings
    USING (app_rls_bypassed() OR business_id = app_current_business())
    WITH CHECK (app_rls_bypassed() OR business_id = app_current_business());
