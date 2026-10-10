-- Buzón del modo demo: con app.demo.enabled los emails no se envían, se guardan acá para verlos en la app.
-- No es una tabla de un negocio (los emails de la cuenta no tienen negocio): no lleva business_id ni RLS.
-- Fuera del modo demo queda vacía.
CREATE TABLE demo_email (
    id         UUID PRIMARY KEY,
    recipient  VARCHAR(320) NOT NULL,
    subject    VARCHAR(200) NOT NULL,
    text_body  TEXT         NOT NULL,
    html_body  TEXT,
    calendar   TEXT,
    created_at TIMESTAMPTZ  NOT NULL
);

-- El buzón de una dirección, del más nuevo al más viejo.
CREATE INDEX demo_email_recipient_idx ON demo_email (recipient, created_at DESC);
-- Para borrar los viejos.
CREATE INDEX demo_email_created_idx ON demo_email (created_at);
