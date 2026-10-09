-- Buscador de negocios tolerante a errores de tipeo y a las tildes ("barberia" encuentra "Barbería",
-- "palerno" encuentra Palermo).

-- Texto normalizado para comparar: minúsculas y sin tildes. unaccent() no es IMMUTABLE (depende de la
-- configuración); fijando el diccionario se puede usar en índices.
CREATE FUNCTION app_search_text(value TEXT) RETURNS TEXT
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
AS $$ SELECT lower(public.unaccent('public.unaccent'::regdictionary, value)) $$;

-- Cómo se busca cada rubro, para que "barberia" o "peluqueria" encuentren negocios aunque no lo digan
-- en el nombre.
CREATE FUNCTION app_category_words(category TEXT) RETURNS TEXT
    LANGUAGE sql IMMUTABLE PARALLEL SAFE STRICT
AS $$
SELECT CASE category
           WHEN 'BARBERSHOP' THEN 'barberia barber'
           WHEN 'BEAUTY_SALON' THEN 'estetica centro de estetica belleza'
           WHEN 'HAIR_SALON' THEN 'peluqueria'
           WHEN 'NAIL_SALON' THEN 'unas manicuria'
           ELSE ''
       END
$$;

-- Índices por trigramas: la búsqueda compara cada palabra con el nombre y el rubro del negocio, y con el
-- barrio y la ciudad de sus sucursales.
CREATE INDEX business_search_trgm_idx ON business
    USING gin (app_search_text(name || ' ' || app_category_words(category)) gin_trgm_ops)
    WHERE searchable;
CREATE INDEX branch_place_trgm_idx ON branch
    USING gin (app_search_text(coalesce(neighborhood, '') || ' ' || city) gin_trgm_ops);
