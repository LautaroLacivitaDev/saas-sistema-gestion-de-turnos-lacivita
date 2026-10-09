package com.lacivita.turnos.notifications.domain;

import com.lacivita.turnos.shared.domain.InvalidValueException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Asunto y mensaje de un email al cliente, con variables entre llaves ({@code {nombre}}, {@code {hora}}).
 *
 * <p>Es texto, no una plantilla de Thymeleaf: el negocio no puede escribir expresiones que se ejecuten en el
 * servidor. Los valores se reemplazan como texto y el diseño del email los escapa al armar el HTML.
 */
// DECISIÓN: el negocio edita el asunto y el mensaje; el diseño del email, los datos del turno y los botones
// de confirmar, reprogramar y cancelar son fijos. Dejar escribir HTML o Thymeleaf permitiría ejecutar
// código en el servidor (inyección de plantillas) y romper los emails en el celular.
public record TemplateText(String subject, String body) {

    static final int MAX_SUBJECT = 150;
    static final int MAX_BODY = 2000;
    private static final Pattern VARIABLE = Pattern.compile("\\{([^{}]*)}");

    public TemplateText {
        subject = required(subject, MAX_SUBJECT, "invalid_template_subject", "El asunto");
        body = required(body, MAX_BODY, "invalid_template_body", "El mensaje");
        if (subject.contains("\n")) {
            throw new InvalidValueException("invalid_template_subject", "El asunto va en una sola línea.");
        }
        checkVariables(subject);
        checkVariables(body);
    }

    /** El texto con los datos del turno en lugar de las variables. */
    public Filled fill(Map<TemplateVariable, String> values) {
        return new Filled(replace(subject, values), replace(body, values));
    }

    /** Texto listo para enviar. Puede superar los largos máximos: los datos del turno no cuentan. */
    public record Filled(String subject, String body) {}

    private static String replace(String text, Map<TemplateVariable, String> values) {
        Matcher matcher = VARIABLE.matcher(text);
        var result = new StringBuilder();
        while (matcher.find()) {
            var variable = TemplateVariable.byKey(matcher.group(1)).orElseThrow();
            matcher.appendReplacement(result, Matcher.quoteReplacement(values.getOrDefault(variable, "")));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private static void checkVariables(String text) {
        Matcher matcher = VARIABLE.matcher(text);
        while (matcher.find()) {
            String key = matcher.group(1);
            if (TemplateVariable.byKey(key).isEmpty()) {
                throw new InvalidValueException(
                        "unknown_template_variable",
                        "No existe la variable {" + key + "}. Podés usar {nombre}, {servicio}, {barbero},"
                                + " {sucursal}, {direccion}, {hora} y {negocio}.");
            }
        }
    }

    private static String required(String value, int maxLength, String code, String label) {
        if (value == null || value.isBlank()) {
            throw new InvalidValueException(code, label + " es obligatorio.");
        }
        String trimmed = value.replace("\r\n", "\n").strip();
        if (trimmed.length() > maxLength) {
            throw new InvalidValueException(code, label + " puede tener hasta " + maxLength + " caracteres.");
        }
        return trimmed;
    }
}
