package com.lacivita.turnos.business.domain;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Lo que alguien escribió en el buscador, separado en palabras. Las palabras de una letra no ayudan a
 * buscar y se descartan; sin palabras, el buscador lista todos los negocios.
 *
 * @param words hasta {@value #MAX_WORDS} palabras en minúsculas (las tildes se ignoran al comparar)
 */
public record SearchTerms(List<String> words) {

    static final int MAX_LENGTH = 80;
    static final int MAX_WORDS = 6;

    public SearchTerms {
        words = List.copyOf(words);
    }

    public static SearchTerms of(String text) {
        if (text == null || text.isBlank()) {
            return new SearchTerms(List.of());
        }
        String trimmed = text.strip();
        String limited = trimmed.length() > MAX_LENGTH ? trimmed.substring(0, MAX_LENGTH) : trimmed;
        // Como pg_trgm: todo lo que no es letra ni número separa palabras ("corte-barba", "a+b").
        return new SearchTerms(Arrays.stream(limited.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
                .filter(word -> word.length() >= 2)
                .distinct()
                .limit(MAX_WORDS)
                .toList());
    }

    public boolean isEmpty() {
        return words.isEmpty();
    }
}
