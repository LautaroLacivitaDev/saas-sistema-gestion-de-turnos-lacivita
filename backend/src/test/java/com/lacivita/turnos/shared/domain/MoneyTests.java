package com.lacivita.turnos.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MoneyTests {

    @Test
    void amountsAreComparedByValueRegardlessOfScale() {
        assertThat(Money.of("1500")).isEqualTo(Money.of("1500.00"));
        assertThat(Money.of("1500.5").amount()).isEqualByComparingTo("1500.50");
    }

    @Test
    void amountsAddUp() {
        assertThat(Money.of("8000").plus(Money.of("4500.50"))).isEqualTo(Money.of("12500.50"));
    }

    @Test
    void amountsCompare() {
        assertThat(Money.of("100").isLessThan(Money.of("100.01"))).isTrue();
        assertThat(Money.of("100").isGreaterThan(Money.of("100.00"))).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "10.005", "100000000"})
    void negativeTooPreciseOrHugeAmountsAreRejected(String amount) {
        assertThatThrownBy(() -> new Money(new BigDecimal(amount))).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void zeroIsAValidPrice() {
        assertThat(Money.ZERO.amount()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
