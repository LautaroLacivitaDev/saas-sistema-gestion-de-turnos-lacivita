-- Notificaciones: la bandeja de salida (outbox) con el registro de envíos, los avisos en la app, los
-- recordatorios que elige cada negocio y los textos que personaliza.

-- Bandeja de salida. Cada aviso se guarda en la misma transacción que el cambio del turno y se envía
-- después, con reintentos: si el envío falla, no se pierde. Es también el registro de envíos.
CREATE TABLE notification (
    id                UUID                     PRIMARY KEY,
    business_id       UUID                     NOT NULL REFERENCES business (id) ON DELETE CASCADE,
    appointment_id    UUID                     REFERENCES appointment (id) ON DELETE CASCADE,
    type              VARCHAR(30)              NOT NULL
        CHECK (type IN ('APPOINTMENT_BOOKED', 'APPOINTMENT_RESCHEDULED', 'APPOINTMENT_CANCELLED',
                        'APPOINTMENT_REMINDER', 'DAILY_AGENDA')),
    audience          VARCHAR(20)              NOT NULL CHECK (audience IN ('CUSTOMER', 'BARBER')),
    channel           VARCHAR(20)              NOT NULL CHECK (channel IN ('EMAIL')),
    -- Profesional que recibe el aviso. Al cliente se lo encuentra por el turno.
    recipient_user_id UUID                     REFERENCES user_account (id) ON DELETE CASCADE,
    -- Día del resumen de agenda, en la zona horaria de la sucursal.
    agenda_date       DATE,
    due_at            TIMESTAMP WITH TIME ZONE NOT NULL,
    next_attempt_at   TIMESTAMP WITH TIME ZONE NOT NULL,
    status            VARCHAR(20)              NOT NULL
        CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'CANCELLED', 'SKIPPED')),
    attempts          INTEGER                  NOT NULL DEFAULT 0 CHECK (attempts >= 0),
    -- Mientras un envío está en curso, ningún otro servidor lo toma.
    locked_until      TIMESTAMP WITH TIME ZONE,
    sent_at           TIMESTAMP WITH TIME ZONE,
    -- Código técnico del último error, sin datos personales.
    last_error        VARCHAR(200),
    created_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at        TIMESTAMP WITH TIME ZONE NOT NULL,
    version           BIGINT                   NOT NULL DEFAULT 0,
    CHECK ((type = 'DAILY_AGENDA') = (appointment_id IS NULL)),
    CHECK ((type = 'DAILY_AGENDA') = (agenda_date IS NOT NULL)),
    CHECK ((audience = 'BARBER') = (recipient_user_id IS NOT NULL)),
    CHECK ((status = 'SENT') = (sent_at IS NOT NULL))
);

-- Lo que hay que enviar, en orden.
CREATE INDEX notification_pending_idx ON notification (next_attempt_at) WHERE status = 'PENDING';
CREATE INDEX notification_appointment_idx ON notification (appointment_id);
-- Un solo resumen de agenda por profesional y por día.
CREATE UNIQUE INDEX notification_daily_agenda_uk ON notification (business_id, recipient_user_id, agenda_date)
    WHERE type = 'DAILY_AGENDA';

-- Avisos que el equipo ve dentro de la app.
CREATE TABLE app_notice (
    id             UUID                     PRIMARY KEY,
    business_id    UUID                     NOT NULL REFERENCES business (id) ON DELETE CASCADE,
    user_id        UUID                     NOT NULL REFERENCES user_account (id) ON DELETE CASCADE,
    appointment_id UUID                     NOT NULL REFERENCES appointment (id) ON DELETE CASCADE,
    type           VARCHAR(30)              NOT NULL
        CHECK (type IN ('APPOINTMENT_BOOKED', 'APPOINTMENT_RESCHEDULED', 'APPOINTMENT_CANCELLED')),
    message        VARCHAR(300)             NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    read_at        TIMESTAMP WITH TIME ZONE
);

CREATE INDEX app_notice_user_idx ON app_notice (business_id, user_id, created_at DESC);
CREATE INDEX app_notice_unread_idx ON app_notice (business_id, user_id) WHERE read_at IS NULL;

-- Cuántas horas antes del turno se le recuerda al cliente.
CREATE TABLE notification_settings (
    business_id    UUID                     PRIMARY KEY REFERENCES business (id) ON DELETE CASCADE,
    reminder_hours INTEGER[]                NOT NULL CHECK (cardinality(reminder_hours) <= 3),
    updated_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    version        BIGINT                   NOT NULL DEFAULT 0
);

-- Textos que el negocio personaliza para los emails a sus clientes. Sin fila, rige el texto de Laciturnos.
CREATE TABLE message_template (
    id          UUID                     PRIMARY KEY,
    business_id UUID                     NOT NULL REFERENCES business (id) ON DELETE CASCADE,
    type        VARCHAR(30)              NOT NULL
        CHECK (type IN ('APPOINTMENT_BOOKED', 'APPOINTMENT_RESCHEDULED', 'APPOINTMENT_CANCELLED',
                        'APPOINTMENT_REMINDER')),
    subject     VARCHAR(150)             NOT NULL,
    body        VARCHAR(2000)            NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    version     BIGINT                   NOT NULL DEFAULT 0,
    UNIQUE (business_id, type)
);

ALTER TABLE notification ENABLE ROW LEVEL SECURITY;
CREATE POLICY notification_access ON notification
    USING (app_rls_bypassed() OR business_id = app_current_business())
    WITH CHECK (app_rls_bypassed() OR business_id = app_current_business());

ALTER TABLE app_notice ENABLE ROW LEVEL SECURITY;
CREATE POLICY app_notice_access ON app_notice
    USING (app_rls_bypassed() OR business_id = app_current_business())
    WITH CHECK (app_rls_bypassed() OR business_id = app_current_business());

ALTER TABLE notification_settings ENABLE ROW LEVEL SECURITY;
CREATE POLICY notification_settings_access ON notification_settings
    USING (app_rls_bypassed() OR business_id = app_current_business())
    WITH CHECK (app_rls_bypassed() OR business_id = app_current_business());

ALTER TABLE message_template ENABLE ROW LEVEL SECURITY;
CREATE POLICY message_template_access ON message_template
    USING (app_rls_bypassed() OR business_id = app_current_business())
    WITH CHECK (app_rls_bypassed() OR business_id = app_current_business());
