-- Usuario con el que se conecta la aplicación en las pruebas (copia de docker/postgres/init/01-app-role.sql).
-- No es superusuario ni dueño de las tablas: así PostgreSQL le aplica Row Level Security.
-- Los permisos sobre las tablas los otorgan las migraciones de Flyway (que corren con el usuario dueño).
-- Solo para desarrollo local: en producción el rol lo crea quien administra la base, con otra contraseña.
CREATE ROLE turnos_app LOGIN PASSWORD 'turnos_app' NOSUPERUSER NOCREATEDB NOCREATEROLE NOBYPASSRLS;
