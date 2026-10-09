package com.lacivita.turnos.notifications.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.lacivita.turnos.shared.domain.InvalidValueException;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TemplateTextTests {

    @Test
    void theVariablesAreReplacedWithTheAppointmentData() {
        var text = new TemplateText("Turno en {negocio}", "Hola {nombre}, te esperamos el {hora}.");

        var filled = text.fill(Map.of(
                TemplateVariable.NEGOCIO, "Barbería Sur",
                TemplateVariable.NOMBRE, "Ana",
                TemplateVariable.HORA, "viernes 9 de octubre a las 10:00"));

        assertThat(filled.subject()).isEqualTo("Turno en Barbería Sur");
        assertThat(filled.body()).isEqualTo("Hola Ana, te esperamos el viernes 9 de octubre a las 10:00.");
    }

    @Test
    void dataThatLooksLikeAVariableIsNotReplacedAgain() {
        var text = new TemplateText("Hola", "Hola {nombre}");

        var filled = text.fill(Map.of(TemplateVariable.NOMBRE, "{negocio} $1 \\"));

        assertThat(filled.body()).isEqualTo("Hola {negocio} $1 \\");
    }

    @Test
    void anUnknownVariableIsRejectedWhenSaving() {
        assertThatThrownBy(() -> new TemplateText("Hola", "Hola {cliente}"))
                .isInstanceOf(InvalidValueException.class)
                .hasMessageContaining("{cliente}");
    }

    @Test
    void theSubjectIsOneLineAndBothHaveALimit() {
        assertThatThrownBy(() -> new TemplateText("Hola\nchau", "Mensaje")).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new TemplateText("a".repeat(151), "Mensaje"))
                .isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new TemplateText("Hola", "a".repeat(2001))).isInstanceOf(InvalidValueException.class);
        assertThatThrownBy(() -> new TemplateText(" ", "Mensaje")).isInstanceOf(InvalidValueException.class);
    }

    @Test
    void thereIsADefaultTextForEveryCustomizableEmailButNotForTheTeamSummary() {
        NotificationType.CUSTOMIZABLE.forEach(
                type -> assertThat(MessageTemplate.defaultFor(type)).isNotNull());
        assertThatThrownBy(() -> MessageTemplate.defaultFor(NotificationType.DAILY_AGENDA))
                .isInstanceOf(InvalidValueException.class);
    }
}
