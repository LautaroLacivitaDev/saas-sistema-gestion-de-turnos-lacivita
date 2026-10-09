package com.lacivita.turnos.notifications.infrastructure;

import com.lacivita.turnos.notifications.domain.EmailContent;
import com.lacivita.turnos.notifications.domain.EmailLayout;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Diseño de los emails con Thymeleaf ({@code templates/mail/notification.html}). La plantilla es fija y solo
 * usa {@code th:text}, que escapa todo: los textos del negocio y los datos del cliente nunca se interpretan
 * como HTML ni como expresiones.
 */
@Component
class ThymeleafEmailLayout implements EmailLayout {

    private static final String TEMPLATE = "mail/notification";
    private static final Locale ES_AR = Locale.of("es", "AR");

    private final ITemplateEngine templates;

    ThymeleafEmailLayout(ITemplateEngine templates) {
        this.templates = templates;
    }

    @Override
    public String html(EmailContent content) {
        var context = new Context(ES_AR);
        context.setVariable("email", content);
        return templates.process(TEMPLATE, context);
    }
}
