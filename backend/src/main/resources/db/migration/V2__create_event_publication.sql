-- Registro de eventos de Spring Modulith (outbox de eventos entre módulos).
-- Cada evento publicado queda guardado en la misma transacción que lo originó
-- hasta que su listener lo procesa, así no se pierde si algo falla.
-- Estructura tomada del mapeo JPA de spring-modulith-events-jpa 2.1.
CREATE TABLE event_publication (
    id                     UUID                     NOT NULL PRIMARY KEY,
    listener_id            TEXT                     NOT NULL,
    event_type             TEXT                     NOT NULL,
    serialized_event       TEXT                     NOT NULL,
    publication_date       TIMESTAMP WITH TIME ZONE NOT NULL,
    completion_date        TIMESTAMP WITH TIME ZONE,
    last_resubmission_date TIMESTAMP WITH TIME ZONE,
    completion_attempts    INTEGER                  NOT NULL DEFAULT 0,
    status                 TEXT CHECK (status IN ('PUBLISHED', 'PROCESSING', 'COMPLETED', 'FAILED', 'RESUBMITTED'))
);

CREATE INDEX event_publication_completion_date_idx ON event_publication (completion_date);
CREATE INDEX event_publication_status_idx ON event_publication (status);
