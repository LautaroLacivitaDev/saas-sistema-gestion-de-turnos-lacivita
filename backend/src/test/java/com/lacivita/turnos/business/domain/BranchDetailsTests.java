package com.lacivita.turnos.business.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lacivita.turnos.shared.domain.InvalidValueException;
import com.lacivita.turnos.shared.domain.PhoneNumber;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class BranchDetailsTests {

    @Test
    void coordinatesAreRoundedToSixDecimals() {
        var coordinates = new Coordinates(new BigDecimal("-34.60372251"), new BigDecimal("-58.38157249"));

        assertThat(coordinates.latitude()).isEqualByComparingTo("-34.603723");
        assertThat(coordinates.longitude()).isEqualByComparingTo("-58.381572");
    }

    @Test
    void coordinatesOutOfRangeAreRejected() {
        assertThatThrownBy(() -> new Coordinates(BigDecimal.valueOf(91), BigDecimal.ZERO))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new Coordinates(BigDecimal.ZERO, null)).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void anAddressNeedsStreetAndCityAndDropsABlankNeighborhood() {
        var address = new Address(" Av. Corrientes 1234 ", "  ", "CABA");

        assertThat(address.street()).isEqualTo("Av. Corrientes 1234");
        assertThat(address.neighborhood()).isNull();
        assertThatThrownBy(() -> new Address("Calle 1", null, " ")).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void phoneNumbersAreNormalizedToDigits() {
        assertThat(new PhoneNumber("+54 9 (11) 4567-8901").value()).isEqualTo("+5491145678901");
        assertThatThrownBy(() -> new PhoneNumber("llamame")).isInstanceOf(InvalidValueException.class);
    }
}
