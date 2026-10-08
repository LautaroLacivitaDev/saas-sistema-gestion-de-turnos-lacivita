package com.lacivita.turnos.users.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lacivita.turnos.shared.domain.InvalidValueException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class NewPasswordTests {

    @Test
    void acceptsPasswordsWithinTheAllowedLength() {
        assertThat(new NewPassword("12345678").value()).isEqualTo("12345678");
        assertThat(new NewPassword("x".repeat(64)).value()).hasSize(64);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"1234567", "        "})
    void rejectsShortOrBlankPasswords(String value) {
        assertThatThrownBy(() -> new NewPassword(value)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void rejectsPasswordsBeyondTheBcryptLimit() {
        assertThatThrownBy(() -> new NewPassword("x".repeat(65))).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void neverPrintsTheValue() {
        assertThat(new NewPassword("secreto-123").toString()).doesNotContain("secreto");
    }
}
