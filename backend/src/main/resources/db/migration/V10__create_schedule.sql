-- Agenda: horario de atención de cada sucursal, horario de cada profesional en cada sucursal,
-- feriados, bloqueos y reglas del negocio para ofrecer horarios.
-- Los días de la semana van de 1 (lunes) a 7 (domingo), como en ISO 8601.

-- Rango de horas del día, para que PostgreSQL detecte franjas superpuestas.
CREATE TYPE timerange AS RANGE (subtype = time);

CREATE TABLE branch_opening (
    id          UUID    PRIMARY KEY,
    business_id UUID    NOT NULL REFERENCES business (id) ON DELETE CASCADE,
    branch_id   UUID    NOT NULL REFERENCES branch (id) ON DELETE CASCADE,
    weekday     INTEGER NOT NULL CHECK (weekday BETWEEN 1 AND 7),
    starts_at   TIME    NOT NULL,
    ends_at     TIME    NOT NULL,
    CHECK (ends_at > starts_at),
    CONSTRAINT branch_opening_no_overlap EXCLUDE USING gist (
        branch_id WITH =,
        weekday WITH =,
        timerange(starts_at, ends_at) WITH &&)
);

-- Un profesional no puede trabajar a la misma hora en dos sucursales del negocio. La restricción de
-- exclusión lo garantiza aunque dos personas guarden horarios al mismo tiempo.
CREATE TABLE work_shift (
    id          UUID    PRIMARY KEY,
    business_id UUID    NOT NULL REFERENCES business (id) ON DELETE CASCADE,
    barber_id   UUID    NOT NULL REFERENCES user_account (id),
    branch_id   UUID    NOT NULL REFERENCES branch (id) ON DELETE CASCADE,
    weekday     INTEGER NOT NULL CHECK (weekday BETWEEN 1 AND 7),
    starts_at   TIME    NOT NULL,
    ends_at     TIME    NOT NULL,
    CHECK (ends_at > starts_at),
    CONSTRAINT work_shift_no_overlap EXCLUDE USING gist (
        business_id WITH =,
        barber_id WITH =,
        weekday WITH =,
        timerange(starts_at, ends_at) WITH &&)
);

CREATE INDEX work_shift_branch_weekday_idx ON work_shift (branch_id, weekday);

-- Sin sucursal: feriado de todo el negocio.
CREATE TABLE holiday (
    id          UUID        PRIMARY KEY,
    business_id UUID        NOT NULL REFERENCES business (id) ON DELETE CASCADE,
    branch_id   UUID        REFERENCES branch (id) ON DELETE CASCADE,
    date        DATE        NOT NULL,
    name        VARCHAR(80) NOT NULL
);

CREATE UNIQUE INDEX holiday_uk
    ON holiday (business_id, COALESCE(branch_id, '00000000-0000-0000-0000-000000000000'::UUID), date);

-- Bloqueo de un profesional o de una sucursal entera (exactamente uno de los dos).
CREATE TABLE time_block (
    id          UUID                     PRIMARY KEY,
    business_id UUID                     NOT NULL REFERENCES business (id) ON DELETE CASCADE,
    barber_id   UUID                     REFERENCES user_account (id),
    branch_id   UUID                     REFERENCES branch (id) ON DELETE CASCADE,
    starts_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    reason      VARCHAR(120),
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    CHECK (ends_at > starts_at),
    CHECK ((barber_id IS NULL) <> (branch_id IS NULL))
);

CREATE INDEX time_block_barber_idx ON time_block (barber_id, starts_at) WHERE barber_id IS NOT NULL;
CREATE INDEX time_block_branch_idx ON time_block (branch_id, starts_at) WHERE branch_id IS NOT NULL;
CREATE INDEX time_block_business_idx ON time_block (business_id, starts_at);

-- Reglas del negocio. Sin fila, rigen los valores por defecto del código.
CREATE TABLE schedule_settings (
    business_id        UUID                     PRIMARY KEY REFERENCES business (id) ON DELETE CASCADE,
    buffer_minutes     INTEGER                  NOT NULL CHECK (buffer_minutes BETWEEN 0 AND 120),
    min_notice_minutes INTEGER                  NOT NULL CHECK (min_notice_minutes >= 0),
    max_advance_days   INTEGER                  NOT NULL CHECK (max_advance_days BETWEEN 1 AND 365),
    slot_step_minutes  INTEGER                  NOT NULL CHECK (slot_step_minutes > 0),
    updated_at         TIMESTAMP WITH TIME ZONE NOT NULL,
    version            BIGINT                   NOT NULL DEFAULT 0
);

-- Aislamiento: todo se lee y se escribe dentro del negocio (la disponibilidad pública también se
-- calcula dentro del negocio del link).
ALTER TABLE branch_opening ENABLE ROW LEVEL SECURITY;
CREATE POLICY branch_opening_access ON branch_opening
    USING (app_rls_bypassed() OR business_id = app_current_business())
    WITH CHECK (app_rls_bypassed() OR business_id = app_current_business());

ALTER TABLE work_shift ENABLE ROW LEVEL SECURITY;
CREATE POLICY work_shift_access ON work_shift
    USING (app_rls_bypassed() OR business_id = app_current_business())
    WITH CHECK (app_rls_bypassed() OR business_id = app_current_business());

ALTER TABLE holiday ENABLE ROW LEVEL SECURITY;
CREATE POLICY holiday_access ON holiday
    USING (app_rls_bypassed() OR business_id = app_current_business())
    WITH CHECK (app_rls_bypassed() OR business_id = app_current_business());

ALTER TABLE time_block ENABLE ROW LEVEL SECURITY;
CREATE POLICY time_block_access ON time_block
    USING (app_rls_bypassed() OR business_id = app_current_business())
    WITH CHECK (app_rls_bypassed() OR business_id = app_current_business());

ALTER TABLE schedule_settings ENABLE ROW LEVEL SECURITY;
CREATE POLICY schedule_settings_access ON schedule_settings
    USING (app_rls_bypassed() OR business_id = app_current_business())
    WITH CHECK (app_rls_bypassed() OR business_id = app_current_business());
