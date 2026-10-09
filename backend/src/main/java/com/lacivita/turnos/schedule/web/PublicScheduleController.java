package com.lacivita.turnos.schedule.web;

import com.lacivita.turnos.catalog.BookableItem;
import com.lacivita.turnos.schedule.application.PublicSchedule;
import com.lacivita.turnos.shared.domain.InvalidValueException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Agenda de la página pública de reservas. No exige sesión. */
@Tag(name = "Página pública")
@RestController
@RequestMapping("/api/public/businesses/{slug}")
class PublicScheduleController {

    private final PublicSchedule schedule;

    PublicScheduleController(PublicSchedule schedule) {
        this.schedule = schedule;
    }

    @Operation(summary = "Horario de atención de una sucursal")
    @GetMapping("/branches/{branchId}/hours")
    ScheduleResponses.WeeklyHours branchHours(@PathVariable String slug, @PathVariable UUID branchId) {
        return ScheduleResponses.WeeklyHours.from(schedule.branchHours(slug, branchId));
    }

    @Operation(
            summary = "Horarios libres de un día",
            description = "Para un servicio (serviceId) o un combo (comboId) en una sucursal. Sin barberId es"
                    + " \"cualquiera disponible\": cada horario trae los profesionales libres, con su precio y su"
                    + " duración.")
    @GetMapping("/availability")
    ScheduleResponses.Availability availability(
            @PathVariable String slug,
            @RequestParam UUID branchId,
            @RequestParam LocalDate date,
            @RequestParam(required = false) UUID serviceId,
            @RequestParam(required = false) UUID comboId,
            @RequestParam(required = false) UUID barberId) {
        return ScheduleResponses.Availability.from(
                schedule.availability(slug, branchId, item(serviceId, comboId), barberId, date));
    }

    private static BookableItem item(UUID serviceId, UUID comboId) {
        if ((serviceId == null) == (comboId == null)) {
            throw new InvalidValueException("invalid_item", "Elegí un servicio o un combo.");
        }
        return serviceId != null ? new BookableItem.ServiceItem(serviceId) : new BookableItem.ComboItem(comboId);
    }
}
