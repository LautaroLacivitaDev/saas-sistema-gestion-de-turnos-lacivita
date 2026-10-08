package com.lacivita.turnos.business.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lacivita.turnos.shared.domain.InvalidValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class SlugTests {

    @ParameterizedTest
    @ValueSource(strings = {"barberia-don-pepe", "la-esquina", "abc", "estetica24", "a1-b2-c3"})
    void acceptsLowercaseLettersNumbersAndSingleHyphens(String value) {
        assertThat(new Slug(value).value()).isEqualTo(value);
    }

    @Test
    void isNormalizedToLowercaseWithoutSurroundingSpaces() {
        assertThat(new Slug("  Barberia-Centro ").value()).isEqualTo("barberia-centro");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(
            strings = {"ab", "con espacio", "peluquería", "-inicio", "fin-", "doble--guion", "guion_bajo", "punto.com"})
    void rejectsInvalidFormats(String value) {
        assertThatThrownBy(() -> new Slug(value))
                .isInstanceOf(InvalidValueException.class)
                .extracting(ex -> ((InvalidValueException) ex).code())
                .isEqualTo("invalid_slug");
    }

    @Test
    void rejectsSlugsLongerThanTheLimit() {
        assertThatThrownBy(() -> new Slug("a".repeat(51))).isInstanceOf(InvalidValueException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"admin", "api", "login", "buscar", "soporte", "ingresar", "verificar-email", "Admin"})
    void rejectsReservedWordsThatWouldHideApplicationPages(String value) {
        assertThatThrownBy(() -> new Slug(value))
                .isInstanceOf(InvalidValueException.class)
                .extracting(ex -> ((InvalidValueException) ex).code())
                .isEqualTo("reserved_slug");
    }
}
