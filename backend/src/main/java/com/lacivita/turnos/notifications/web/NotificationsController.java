package com.lacivita.turnos.notifications.web;

import com.lacivita.turnos.notifications.application.DeliveryLog;
import com.lacivita.turnos.notifications.application.MessageTemplates;
import com.lacivita.turnos.notifications.application.NotificationSettingsService;
import com.lacivita.turnos.notifications.application.NotificationViews.DeliveryView;
import com.lacivita.turnos.notifications.application.NotificationViews.SettingsView;
import com.lacivita.turnos.notifications.application.NotificationViews.TemplateView;
import com.lacivita.turnos.notifications.application.NotificationViews.TemplatesView;
import com.lacivita.turnos.notifications.domain.NotificationType;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notificaciones")
@RestController
@RequestMapping("/api/businesses/{businessId}")
class NotificationsController {

    private final DeliveryLog deliveryLog;
    private final NotificationSettingsService settings;
    private final MessageTemplates templates;

    NotificationsController(DeliveryLog deliveryLog, NotificationSettingsService settings, MessageTemplates templates) {
        this.deliveryLog = deliveryLog;
        this.settings = settings;
        this.templates = templates;
    }

    @Operation(
            summary = "Registro de envíos de un turno",
            description = "Avisos al cliente y al profesional con su estado: PENDING, SENT, FAILED, CANCELLED o"
                    + " SKIPPED. Lo ve quien puede ver el turno.")
    @GetMapping("/appointments/{appointmentId}/notifications")
    List<DeliveryView> deliveries(
            @PathVariable UUID businessId,
            @PathVariable UUID appointmentId,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return deliveryLog.of(businessId, actor, appointmentId);
    }

    @Operation(summary = "Recordatorios al cliente", description = "Todo el equipo.")
    @GetMapping("/notification-settings")
    SettingsView settings(@PathVariable UUID businessId) {
        return settings.of(businessId);
    }

    @Operation(
            summary = "Cambia los recordatorios al cliente",
            description = "Solo el dueño. Hasta 3, entre 1 y 168 horas antes del turno; una lista vacía los"
                    + " desactiva. Rige para los turnos que se reserven o se muevan desde ahora.")
    @PutMapping("/notification-settings")
    SettingsView changeSettings(
            @PathVariable UUID businessId, @Valid @RequestBody NotificationRequests.SettingsData body) {
        return settings.change(businessId, body.reminders());
    }

    @Operation(
            summary = "Textos de los emails al cliente",
            description = "Gerentes y dueño. Los que rigen (propios o de Laciturnos) y las variables disponibles.")
    @GetMapping("/message-templates")
    TemplatesView templates(@PathVariable UUID businessId) {
        return templates.of(businessId);
    }

    @Operation(
            summary = "Personaliza el texto de un email al cliente",
            description = "Solo el dueño. Asunto (hasta 150 caracteres) y mensaje (hasta 2000), con variables entre"
                    + " llaves. El diseño, los datos del turno y los botones no cambian.")
    @PutMapping("/message-templates/{type}")
    TemplateView changeTemplate(
            @PathVariable UUID businessId,
            @PathVariable NotificationType type,
            @RequestBody NotificationRequests.TemplateData body) {
        return templates.change(businessId, type, body.text());
    }

    @Operation(summary = "Vuelve al texto de Laciturnos", description = "Solo el dueño.")
    @DeleteMapping("/message-templates/{type}")
    TemplateView resetTemplate(@PathVariable UUID businessId, @PathVariable NotificationType type) {
        return templates.reset(businessId, type);
    }
}
