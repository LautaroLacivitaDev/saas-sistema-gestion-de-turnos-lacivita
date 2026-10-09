package com.lacivita.turnos.notifications.web;

import com.lacivita.turnos.notifications.domain.ReminderSchedule;
import com.lacivita.turnos.notifications.domain.TemplateText;
import jakarta.validation.constraints.NotNull;
import java.util.List;

final class NotificationRequests {

    private NotificationRequests() {}

    /** @param reminderHours horas antes del turno, hasta 3 (por ejemplo [24, 2]); vacío los desactiva */
    record SettingsData(
            @NotNull(message = "Indicá los recordatorios.") List<Integer> reminderHours) {

        ReminderSchedule reminders() {
            return new ReminderSchedule(reminderHours);
        }
    }

    /** Las variables van entre llaves: {nombre}, {servicio}, {barbero}, {sucursal}, {direccion}, {hora}, {negocio}. */
    record TemplateData(String subject, String body) {

        TemplateText text() {
            return new TemplateText(subject, body);
        }
    }
}
