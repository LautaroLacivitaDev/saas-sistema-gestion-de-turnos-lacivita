package com.lacivita.turnos.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class EmailTests {

    @Test
    void isNormalizedToLowercaseWithoutSurroundingSpaces() {
        var email = new Email("  Lautaro.Perez@Example.COM ");

        assertThat(email.value()).isEqualTo("lautaro.perez@example.com");
    }

    @Test
    void emailsThatOnlyDifferInCaseAreEqual() {
        assertThat(new Email("ana@example.com")).isEqualTo(new Email("ANA@example.com"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "sin-arroba.com", "ana@", "@example.com", "ana@example", "ana perez@example.com"})
    void invalidAddressesAreRejected(String value) {
        assertThatThrownBy(() -> new Email(value))
                .isInstanceOf(InvalidValueException.class)
                .extracting(ex -> ((InvalidValueException) ex).code())
                .isEqualTo("invalid_email");
    }

    @Test
    void addressesLongerThanTheStandardLimitAreRejected() {
        String tooLong = "a".repeat(250) + "@x.com";

        assertThatThrownBy(() -> new Email(tooLong)).isInstanceOf(InvalidValueException.class);
    }
}
