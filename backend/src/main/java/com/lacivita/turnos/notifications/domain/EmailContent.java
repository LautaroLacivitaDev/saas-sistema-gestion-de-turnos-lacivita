package com.lacivita.turnos.notifications.domain;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Lo que dice un email, sin el diseño: el diseño lo pone {@link EmailLayout}. El mismo contenido sirve para
 * la versión en texto plano, que leen los clientes de correo sin HTML y los filtros de spam.
 *
 * @param paragraphs párrafos del mensaje, en texto (el diseño los escapa)
 * @param details datos del turno, por ejemplo "Día y hora: viernes 9 de octubre a las 10:00"
 * @param items lista de renglones, por ejemplo los turnos del día en el resumen de agenda
 * @param actions botones con links, por ejemplo "Cancelar turno"
 * @param footer aclaración al pie, por ejemplo quién envía el email
 */
public record EmailContent(
        String subject,
        List<String> paragraphs,
        List<Detail> details,
        List<String> items,
        List<Action> actions,
        String footer) {

    public EmailContent {
        Objects.requireNonNull(subject, "subject");
        paragraphs = List.copyOf(paragraphs);
        details = List.copyOf(details);
        items = List.copyOf(items);
        actions = List.copyOf(actions);
        Objects.requireNonNull(footer, "footer");
    }

    public record Detail(String label, String value) {}

    public record Action(String label, String url) {}

    public String plainText() {
        var text = new StringBuilder();
        paragraphs.forEach(paragraph -> text.append(paragraph).append("\n\n"));
        if (!details.isEmpty()) {
            text.append(details.stream()
                            .map(detail -> detail.label() + ": " + detail.value())
                            .collect(Collectors.joining("\n")))
                    .append("\n\n");
        }
        if (!items.isEmpty()) {
            text.append(items.stream().map(item -> "- " + item).collect(Collectors.joining("\n")))
                    .append("\n\n");
        }
        actions.forEach(action ->
                text.append(action.label()).append(": ").append(action.url()).append("\n"));
        if (!actions.isEmpty()) {
            text.append("\n");
        }
        return text.append(footer).append("\n").toString();
    }
}
