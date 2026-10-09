-- Tablas de JobRunr 8.8 (tareas en segundo plano y programadas), en el estado final de sus migraciones
-- v000 a v016 para PostgreSQL.
--
-- Las crea Flyway, no JobRunr: la aplicación se conecta con un rol sin permiso para crear tablas, y así
-- todas las migraciones quedan en un solo lugar. JobRunr arranca con jobrunr.database.skip-create=true.
-- Al actualizar JobRunr, revisar si trae migraciones nuevas (org/jobrunr/storage/sql/{common,postgres})
-- y agregarlas en una migración nueva, junto con su fila en jobrunr_migrations.

CREATE TABLE jobrunr_migrations
(
    id          NCHAR(36) PRIMARY KEY,
    script      VARCHAR(64) NOT NULL,
    installedOn VARCHAR(29) NOT NULL
);

CREATE TABLE jobrunr_jobs
(
    id             NCHAR(36) PRIMARY KEY,
    version        INT          NOT NULL,
    jobAsJson      TEXT         NOT NULL,
    jobSignature   VARCHAR(512) NOT NULL,
    state          VARCHAR(36)  NOT NULL,
    createdAt      TIMESTAMP    NOT NULL,
    updatedAt      TIMESTAMP    NOT NULL,
    scheduledAt    TIMESTAMP,
    recurringJobId VARCHAR(128)
);
CREATE INDEX jobrunr_state_idx ON jobrunr_jobs (state);
CREATE INDEX jobrunr_job_signature_idx ON jobrunr_jobs (jobSignature);
CREATE INDEX jobrunr_job_created_at_idx ON jobrunr_jobs (createdAt);
CREATE INDEX jobrunr_job_scheduled_at_idx ON jobrunr_jobs (scheduledAt);
CREATE INDEX jobrunr_job_rci_idx ON jobrunr_jobs (recurringJobId);
CREATE INDEX jobrunr_jobs_state_updated_idx ON jobrunr_jobs (state ASC, updatedAt ASC);

CREATE TABLE jobrunr_recurring_jobs
(
    id        NCHAR(128) PRIMARY KEY,
    version   INT    NOT NULL,
    jobAsJson TEXT   NOT NULL,
    createdAt BIGINT NOT NULL DEFAULT '0'
);
CREATE INDEX jobrunr_recurring_job_created_at_idx ON jobrunr_recurring_jobs (createdAt);

CREATE TABLE jobrunr_backgroundjobservers
(
    id                         NCHAR(36) PRIMARY KEY,
    workerPoolSize             INT           NOT NULL,
    pollIntervalInSeconds      INT           NOT NULL,
    firstHeartbeat             TIMESTAMP(6)  NOT NULL,
    lastHeartbeat              TIMESTAMP(6)  NOT NULL,
    running                    INT           NOT NULL,
    systemTotalMemory          BIGINT        NOT NULL,
    systemFreeMemory           BIGINT        NOT NULL,
    systemCpuLoad              NUMERIC(3, 2) NOT NULL,
    processMaxMemory           BIGINT        NOT NULL,
    processFreeMemory          BIGINT        NOT NULL,
    processAllocatedMemory     BIGINT        NOT NULL,
    processCpuLoad             NUMERIC(3, 2) NOT NULL,
    deleteSucceededJobsAfter   VARCHAR(32),
    permanentlyDeleteJobsAfter VARCHAR(32),
    name                       VARCHAR(128)
);
CREATE INDEX jobrunr_bgjobsrvrs_fsthb_idx ON jobrunr_backgroundjobservers (firstHeartbeat);
CREATE INDEX jobrunr_bgjobsrvrs_lsthb_idx ON jobrunr_backgroundjobservers (lastHeartbeat);

CREATE TABLE jobrunr_metadata
(
    id        VARCHAR(156) PRIMARY KEY,
    name      VARCHAR(92) NOT NULL,
    owner     VARCHAR(64) NOT NULL,
    value     TEXT        NOT NULL,
    createdAt TIMESTAMP   NOT NULL,
    updatedAt TIMESTAMP   NOT NULL
);

INSERT INTO jobrunr_metadata (id, name, owner, value, createdAt, updatedAt)
VALUES ('succeeded-jobs-counter-cluster', 'succeeded-jobs-counter', 'cluster', '0', CURRENT_TIMESTAMP,
        CURRENT_TIMESTAMP);

CREATE VIEW jobrunr_jobs_stats
AS
WITH job_stat_results AS (SELECT state, count(*) AS count
                          FROM jobrunr_jobs
                          GROUP BY state)
SELECT coalesce((SELECT sum(job_stat_results.count) FROM job_stat_results), 0)                            AS total,
       coalesce((SELECT sum(job_stat_results.count) FROM job_stat_results WHERE state = 'AWAITING'), 0)   AS awaiting,
       coalesce((SELECT sum(job_stat_results.count) FROM job_stat_results WHERE state = 'SCHEDULED'), 0)  AS scheduled,
       coalesce((SELECT sum(job_stat_results.count) FROM job_stat_results WHERE state = 'ENQUEUED'), 0)   AS enqueued,
       coalesce((SELECT sum(job_stat_results.count) FROM job_stat_results WHERE state = 'PROCESSING'), 0) AS processing,
       coalesce((SELECT sum(job_stat_results.count) FROM job_stat_results WHERE state = 'PROCESSED'), 0)  AS processed,
       coalesce((SELECT sum(job_stat_results.count) FROM job_stat_results WHERE state = 'FAILED'), 0)     AS failed,
       coalesce((SELECT sum(job_stat_results.count) FROM job_stat_results WHERE state = 'SUCCEEDED'), 0)  AS succeeded,
       coalesce((SELECT cast(cast(value AS CHAR(10)) AS DECIMAL(10, 0))
                 FROM jobrunr_metadata jm
                 WHERE jm.id = 'succeeded-jobs-counter-cluster'), 0)                                     AS allTimeSucceeded,
       coalesce((SELECT sum(job_stat_results.count) FROM job_stat_results WHERE state = 'DELETED'), 0)    AS deleted,
       (SELECT count(*) FROM jobrunr_backgroundjobservers)                                                AS nbrOfBackgroundJobServers,
       (SELECT count(*) FROM jobrunr_recurring_jobs)                                                      AS nbrOfRecurringJobs;

-- Las migraciones de JobRunr que este script ya cubre.
INSERT INTO jobrunr_migrations (id, script, installedOn)
SELECT gen_random_uuid()::TEXT, script, to_char(now() AT TIME ZONE 'UTC', 'YYYY-MM-DD"T"HH24:MI:SS.US')
FROM unnest(ARRAY [
    'v000__create_migrations_table.sql',
    'v001__create_job_table.sql',
    'v002__create_recurring_job_table.sql',
    'v003__create_background_job_server_table.sql',
    'v004__create_job_stats_view.sql',
    'v005__update_job_stats_view.sql',
    'v006__alter_table_jobs_add_recurringjob.sql',
    'v007__alter_table_backgroundjobserver_add_delete_config.sql',
    'v008__alter_table_jobs_increase_jobAsJson_size.sql',
    'v009__change_jobrunr_job_counters_to_jobrunr_metadata.sql',
    'v010__change_job_stats.sql',
    'v011__change_sqlserver_text_to_varchar.sql',
    'v012__change_oracle_alter_jobrunr_metadata_column_size.sql',
    'v013__alter_table_recurring_job_add_createdAt.sql',
    'v014__improve_job_stats.sql',
    'v015__alter_table_backgroundjobserver_add_name.sql',
    'v016__alter_jobs_stats_add_awaiting_jobs.sql'
    ]) AS script;
