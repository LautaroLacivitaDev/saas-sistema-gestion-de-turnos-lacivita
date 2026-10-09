package com.lacivita.turnos.notifications.application;

import com.lacivita.turnos.notifications.domain.AppNotice;
import com.lacivita.turnos.notifications.domain.Notification;
import com.lacivita.turnos.notifications.domain.TemplateVariable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Lo que devuelven los casos de uso de notificaciones. Nunca entidades. */
public final class NotificationViews {

    private NotificationViews() {}

    /**
     * Una línea del registro de envíos de un turno.
     *
     * @param recipientUserId profesional que recibe el aviso; nulo si es para el cliente
     * @param dueAt cuándo sale (o salió) el aviso, por ejemplo un recordatorio
     * @param sentAt cuándo lo aceptó el proveedor; nulo si todavía no salió
     */
    public record DeliveryView(
            UUID id,
            String type,
            String audience,
            UUID recipientUserId,
            String channel,
            String status,
            Instant dueAt,
            Instant sentAt,
            int attempts) {

        static DeliveryView of(Notification notification) {
            return new DeliveryView(
                    notification.getId(),
                    notification.getType().name(),
                    notification.getAudience().name(),
                    notification.recipientUserId().orElse(null),
                    notification.getChannel().name(),
                    notification.getStatus().name(),
                    notification.getDueAt(),
                    notification.sentAt().orElse(null),
                    notification.getAttempts());
        }
    }

    public record NoticeView(
            UUID id, UUID appointmentId, String type, String message, Instant createdAt, boolean read) {

        static NoticeView of(AppNotice notice) {
            return new NoticeView(
                    notice.getId(),
                    notice.getAppointmentId(),
                    notice.getType().name(),
                    notice.getMessage(),
                    notice.getCreatedAt(),
                    notice.readAt().isPresent());
        }
    }

    /** @param reminderHours horas antes del turno en que se le recuerda al cliente */
    public record SettingsView(List<Integer> reminderHours) {}

    /** @param custom {@code true} si el negocio cambió el texto de Laciturnos */
    public record TemplateView(String type, String subject, String body, boolean custom) {}

    public record VariableView(String key, String description) {

        static VariableView of(TemplateVariable variable) {
            return new VariableView(variable.key(), variable.description());
        }
    }

    public record TemplatesView(List<TemplateView> templates, List<VariableView> variables) {}
}
