package com.lacivita.turnos.catalog.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lacivita.turnos.shared.domain.InvalidValueException;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProfessionalProfileTests {

    @Test
    void blankTextsAreTreatedAsMissing() {
        var profile = new ProfessionalProfile("  ", null, "");

        assertThat(profile).isEqualTo(ProfessionalProfile.EMPTY);
    }

    @Test
    void specialtiesAreTrimmedAndCannotRepeatIgnoringCase() {
        assertThat(new ProfessionalProfile(null, List.of(" Fade ", "Barba"), null).specialties())
                .containsExactly("Fade", "Barba");
        assertThatThrownBy(() -> new ProfessionalProfile(null, List.of("Fade", "FADE"), null))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new ProfessionalProfile(null, List.of("a", "b", "c", "d", "e", "f", "g"), null))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void thePhotoIsAnHttpsLink() {
        assertThat(new ProfessionalProfile(null, List.of(), "https://example.com/yo.jpg").photoUrl())
                .isEqualTo("https://example.com/yo.jpg");
        assertThatThrownBy(() -> new ProfessionalProfile(null, List.of(), "http://example.com/yo.jpg"))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new ProfessionalProfile(null, List.of(), "javascript:alert(1)"))
                .isInstanceOf(InvalidValueException.class);
    }
}
