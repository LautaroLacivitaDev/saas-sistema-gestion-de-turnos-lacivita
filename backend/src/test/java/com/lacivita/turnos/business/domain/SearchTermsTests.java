package com.lacivita.turnos.business.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SearchTermsTests {

    @Test
    void theTextIsSplitIntoLowercaseWordsWithoutRepeatsOrSingleLetters() {
        assertThat(SearchTerms.of("  Barbería  el tano, Palermo y barbería ").words())
                .containsExactly("barbería", "el", "tano", "palermo");
    }

    @Test
    void anythingThatIsNotALetterOrNumberSeparatesWords() {
        assertThat(SearchTerms.of("corte-barba+fade/2x1").words()).containsExactly("corte", "barba", "fade", "2x1");
    }

    @Test
    void anEmptySearchListsEverything() {
        assertThat(SearchTerms.of(null).isEmpty()).isTrue();
        assertThat(SearchTerms.of("   ").isEmpty()).isTrue();
    }

    @Test
    void longSearchesAreCut() {
        assertThat(SearchTerms.of("uno dos tres cuatro cinco seis siete ocho").words())
                .hasSize(6);
    }
}
