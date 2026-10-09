package com.lacivita.turnos.notifications.application;

import com.lacivita.turnos.notifications.application.NotificationViews.TemplateView;
import com.lacivita.turnos.notifications.application.NotificationViews.TemplatesView;
import com.lacivita.turnos.notifications.application.NotificationViews.VariableView;
import com.lacivita.turnos.notifications.domain.MessageTemplate;
import com.lacivita.turnos.notifications.domain.MessageTemplateRepository;
import com.lacivita.turnos.notifications.domain.NotificationType;
import com.lacivita.turnos.notifications.domain.TemplateText;
import com.lacivita.turnos.notifications.domain.TemplateVariable;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import java.time.Clock;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Textos de los emails al cliente que el negocio personaliza. */
@Service
public class MessageTemplates {

    private final MessageTemplateRepository templates;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    MessageTemplates(MessageTemplateRepository templates, ApplicationEventPublisher events, Clock clock) {
        this.templates = templates;
        this.events = events;
        this.clock = clock;
    }

    /** Todos los textos que rigen, propios o de Laciturnos, y las variables que se pueden usar. */
    @BusinessScoped
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#businessId, 'Business', 'MANAGER')")
    public TemplatesView of(@BusinessId UUID businessId) {
        Map<NotificationType, MessageTemplate> custom =
                templates.findAll().stream().collect(Collectors.toMap(MessageTemplate::getType, Function.identity()));
        var views = NotificationType.CUSTOMIZABLE.stream()
                .map(type -> custom.containsKey(type)
                        ? view(type, custom.get(type).text(), true)
                        : view(type, MessageTemplate.defaultFor(type), false))
                .toList();
        return new TemplatesView(
                views,
                Arrays.stream(TemplateVariable.values()).map(VariableView::of).toList());
    }

    /** Solo el dueño. */
    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'OWNER')")
    public TemplateView change(@BusinessId UUID businessId, NotificationType type, TemplateText text) {
        var now = clock.instant();
        templates
                .findByType(type)
                .ifPresentOrElse(
                        existing -> existing.change(text, now),
                        () -> templates.save(MessageTemplate.custom(businessId, type, text, now)));
        events.publishEvent(new NotificationEvents.TemplateChanged(businessId, type.name(), text.subject()));
        return view(type, text, true);
    }

    /** Vuelve al texto de Laciturnos. Solo el dueño. */
    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'OWNER')")
    public TemplateView reset(@BusinessId UUID businessId, NotificationType type) {
        var text = MessageTemplate.defaultFor(type);
        templates.findByType(type).ifPresent(existing -> {
            templates.delete(existing);
            events.publishEvent(new NotificationEvents.TemplateChanged(businessId, type.name(), null));
        });
        return view(type, text, false);
    }

    private static TemplateView view(NotificationType type, TemplateText text, boolean custom) {
        return new TemplateView(type.name(), text.subject(), text.body(), custom);
    }
}
