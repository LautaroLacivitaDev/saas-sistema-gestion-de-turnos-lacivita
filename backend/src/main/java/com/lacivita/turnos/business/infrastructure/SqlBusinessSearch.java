package com.lacivita.turnos.business.infrastructure;

import com.lacivita.turnos.business.domain.BusinessSearch;
import com.lacivita.turnos.business.domain.SearchTerms;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Búsqueda con trigramas de PostgreSQL ({@code pg_trgm}) sobre el texto sin tildes (ver V15). Cada palabra
 * tiene que parecerse a alguna palabra del nombre, del rubro o del lugar de una sucursal: así "barbria
 * palermo" encuentra "Barbería El Tano", en Palermo.
 */
// DECISIÓN: se exige que coincidan todas las palabras (no alcanza con una) y el orden es por parecido con el
// nombre. Una palabra se considera parecida desde 0,4 de similitud por trigramas: tolera una letra
// cambiada o faltante en palabras de cinco o más letras.
@Component
class SqlBusinessSearch implements BusinessSearch {

    /** Cuánto se tienen que parecer dos palabras (0 a 1) para el operador {@code <%}. */
    private static final String SIMILARITY = "SET LOCAL pg_trgm.word_similarity_threshold = 0.4";

    private static final String QUERY = """
            SELECT b.id, b.name, b.slug, b.category, b.description
            FROM business b
            CROSS JOIN LATERAL (
                SELECT bool_and(
                           app_search_text(w) <% app_search_text(b.name || ' ' || app_category_words(b.category))
                           OR EXISTS (
                               SELECT 1 FROM branch br
                               WHERE br.business_id = b.id
                                 AND app_search_text(w)
                                     <% app_search_text(coalesce(br.neighborhood, '') || ' ' || br.city))) AS matches,
                       coalesce(sum(word_similarity(app_search_text(w), app_search_text(b.name))), 0) AS score
                FROM unnest(string_to_array(:words, ' ')) AS w
            ) s
            WHERE b.searchable
              AND (:words = '' OR s.matches)
            ORDER BY s.score DESC, b.name, b.id
            LIMIT :limit OFFSET :offset
            """;

    private final JdbcClient jdbc;

    SqlBusinessSearch(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Requiere una transacción: el umbral de parecido se fija solo para ella. */
    @Override
    public List<Hit> search(SearchTerms terms, int limit, int offset) {
        jdbc.sql(SIMILARITY).update();
        return jdbc.sql(QUERY)
                .param("words", String.join(" ", terms.words()))
                .param("limit", limit)
                .param("offset", offset)
                .query(Hit.class)
                .list();
    }
}
