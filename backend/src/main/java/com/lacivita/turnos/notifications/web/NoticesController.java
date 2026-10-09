package com.lacivita.turnos.notifications.web;

import com.lacivita.turnos.notifications.application.Notices;
import com.lacivita.turnos.notifications.application.NotificationViews.NoticeView;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.web.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Avisos en la app")
@RestController
@RequestMapping("/api/businesses/{businessId}/notices")
class NoticesController {

    private static final int MAX_SIZE = 50;

    private final Notices notices;

    NoticesController(Notices notices) {
        this.notices = notices;
    }

    @Operation(summary = "Mis avisos", description = "Del más nuevo al más viejo, de a 20.")
    @GetMapping
    PageResponse<NoticeView> mine(
            @PathVariable UUID businessId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_SIZE));
        return PageResponse.of(notices.mine(businessId, actor, pageable), notice -> notice);
    }

    @Operation(summary = "Cuántos avisos sin leer tengo", description = "Para el número de la campanita.")
    @GetMapping("/unread-count")
    UnreadCount unreadCount(@PathVariable UUID businessId, @AuthenticationPrincipal AuthenticatedUser actor) {
        return new UnreadCount(notices.unreadCount(businessId, actor));
    }

    @Operation(summary = "Marca un aviso como leído")
    @PostMapping("/{noticeId}/read")
    NoticeView markRead(
            @PathVariable UUID businessId,
            @PathVariable UUID noticeId,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return notices.markRead(businessId, actor, noticeId);
    }

    @Operation(summary = "Marca todos mis avisos como leídos")
    @PostMapping("/read-all")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void markAllRead(@PathVariable UUID businessId, @AuthenticationPrincipal AuthenticatedUser actor) {
        notices.markAllRead(businessId, actor);
    }

    record UnreadCount(long unread) {}
}
