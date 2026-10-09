package com.lacivita.turnos.schedule.web;

import com.lacivita.turnos.schedule.application.BranchHours;
import com.lacivita.turnos.schedule.application.Holidays;
import com.lacivita.turnos.schedule.application.ScheduleSettingsService;
import com.lacivita.turnos.schedule.application.TimeBlocks;
import com.lacivita.turnos.schedule.application.WorkSchedules;
import com.lacivita.turnos.shared.domain.TimeInterval;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Agenda")
@RestController
@RequestMapping("/api/businesses/{businessId}")
class ScheduleController {

    private static final int MAX_PAGE_SIZE = 100;

    private final BranchHours branchHours;
    private final WorkSchedules workSchedules;
    private final Holidays holidays;
    private final TimeBlocks timeBlocks;
    private final ScheduleSettingsService settings;

    ScheduleController(
            BranchHours branchHours,
            WorkSchedules workSchedules,
            Holidays holidays,
            TimeBlocks timeBlocks,
            ScheduleSettingsService settings) {
        this.branchHours = branchHours;
        this.workSchedules = workSchedules;
        this.holidays = holidays;
        this.timeBlocks = timeBlocks;
        this.settings = settings;
    }

    @Operation(summary = "Horario de atención de una sucursal")
    @GetMapping("/branches/{branchId}/hours")
    ScheduleResponses.WeeklyHours branchHours(@PathVariable UUID businessId, @PathVariable UUID branchId) {
        return ScheduleResponses.WeeklyHours.from(branchHours.of(businessId, branchId));
    }

    @Operation(
            summary = "Reemplaza el horario de atención de una sucursal",
            description = "Dueño y gerentes de la sucursal. Los días que no se mandan quedan cerrados.")
    @PutMapping("/branches/{branchId}/hours")
    ScheduleResponses.WeeklyHours replaceBranchHours(
            @PathVariable UUID businessId,
            @PathVariable UUID branchId,
            @Valid @RequestBody ScheduleRequests.WeeklyHoursData body,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return ScheduleResponses.WeeklyHours.from(branchHours.replace(businessId, actor, branchId, body.hours()));
    }

    @Operation(summary = "Horario de un profesional en cada una de sus sucursales", description = "Todo el equipo.")
    @GetMapping("/barbers/{barberId}/schedule")
    List<ScheduleResponses.BranchHours> workSchedule(@PathVariable UUID businessId, @PathVariable UUID barberId) {
        return workSchedules.of(businessId, barberId).stream()
                .map(ScheduleResponses.BranchHours::from)
                .toList();
    }

    @Operation(
            summary = "Reemplaza el horario de un profesional en una sucursal",
            description = "Cada uno el suyo; el gerente, el de los barberos de sus sucursales; el dueño, el de todos."
                    + " El descanso es el hueco entre dos franjas. No se puede superponer con su horario en otra"
                    + " sucursal (409 schedule_overlap).")
    @PutMapping("/barbers/{barberId}/schedule/{branchId}")
    ScheduleResponses.WeeklyHours replaceWorkSchedule(
            @PathVariable UUID businessId,
            @PathVariable UUID barberId,
            @PathVariable UUID branchId,
            @Valid @RequestBody ScheduleRequests.WeeklyHoursData body,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return ScheduleResponses.WeeklyHours.from(
                workSchedules.replace(businessId, actor, barberId, branchId, body.hours()));
    }

    @Operation(summary = "Feriados del período", description = "Todo el equipo. Hasta dos años por consulta.")
    @GetMapping("/holidays")
    List<ScheduleResponses.Holiday> holidays(
            @PathVariable UUID businessId, @RequestParam LocalDate from, @RequestParam LocalDate to) {
        return holidays.between(businessId, from, to).stream()
                .map(ScheduleResponses.Holiday::from)
                .toList();
    }

    @Operation(
            summary = "Carga un feriado",
            description = "Sin sucursal, vale para todo el negocio (solo el dueño). Con sucursal, su gerente o el"
                    + " dueño.")
    @PostMapping("/holidays")
    @ResponseStatus(HttpStatus.CREATED)
    ScheduleResponses.Holiday addHoliday(
            @PathVariable UUID businessId,
            @Valid @RequestBody ScheduleRequests.HolidayData body,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return ScheduleResponses.Holiday.from(
                holidays.add(businessId, actor, body.branchId(), body.date(), body.name()));
    }

    @Operation(summary = "Borra un feriado")
    @DeleteMapping("/holidays/{holidayId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeHoliday(
            @PathVariable UUID businessId,
            @PathVariable UUID holidayId,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        holidays.remove(businessId, actor, holidayId);
    }

    @Operation(summary = "Bloqueos que se cruzan con el período", description = "Todo el equipo.")
    @GetMapping("/time-blocks")
    PageResponse<ScheduleResponses.TimeBlock> timeBlocks(
            @PathVariable UUID businessId,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE), Sort.by("startsAt"));
        return PageResponse.of(
                timeBlocks.overlapping(businessId, new TimeInterval(from, to), pageable),
                ScheduleResponses.TimeBlock::from);
    }

    @Operation(
            summary = "Bloquea el horario de un profesional",
            description = "Un trámite, vacaciones. Cada uno los suyos; el gerente, los de los barberos de sus"
                    + " sucursales; el dueño, los de todos.")
    @PostMapping("/barbers/{barberId}/time-blocks")
    @ResponseStatus(HttpStatus.CREATED)
    ScheduleResponses.TimeBlock blockBarber(
            @PathVariable UUID businessId,
            @PathVariable UUID barberId,
            @Valid @RequestBody ScheduleRequests.BlockData body,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return ScheduleResponses.TimeBlock.from(
                timeBlocks.blockBarber(businessId, actor, barberId, body.interval(), body.reason()));
    }

    @Operation(summary = "Bloquea una sucursal entera", description = "Su gerente o el dueño.")
    @PostMapping("/branches/{branchId}/time-blocks")
    @ResponseStatus(HttpStatus.CREATED)
    ScheduleResponses.TimeBlock blockBranch(
            @PathVariable UUID businessId,
            @PathVariable UUID branchId,
            @Valid @RequestBody ScheduleRequests.BlockData body,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return ScheduleResponses.TimeBlock.from(
                timeBlocks.blockBranch(businessId, actor, branchId, body.interval(), body.reason()));
    }

    @Operation(summary = "Quita un bloqueo")
    @DeleteMapping("/time-blocks/{blockId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeBlock(
            @PathVariable UUID businessId,
            @PathVariable UUID blockId,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        timeBlocks.remove(businessId, actor, blockId);
    }

    @Operation(summary = "Reglas de agenda del negocio", description = "Todo el equipo.")
    @GetMapping("/schedule-rules")
    ScheduleResponses.Rules rules(@PathVariable UUID businessId) {
        return ScheduleResponses.Rules.from(settings.rules(businessId));
    }

    @Operation(
            summary = "Cambia las reglas de agenda",
            description = "Solo el dueño: tiempo de preparación entre turnos, anticipación mínima (minutos) y"
                    + " máxima (días) y cada cuántos minutos se ofrecen horarios.")
    @PutMapping("/schedule-rules")
    ScheduleResponses.Rules changeRules(
            @PathVariable UUID businessId, @Valid @RequestBody ScheduleRequests.RulesData body) {
        return ScheduleResponses.Rules.from(settings.change(businessId, body.rules()));
    }
}
