-- Extensiones que usa el sistema:
--   btree_gist: restricción de exclusión contra turnos superpuestos (barbero + rango de tiempo).
--   pg_trgm:    búsqueda de negocios por nombre tolerante a errores de tipeo.
--   unaccent:   búsqueda sin distinguir tildes.
CREATE EXTENSION IF NOT EXISTS btree_gist;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS unaccent;
