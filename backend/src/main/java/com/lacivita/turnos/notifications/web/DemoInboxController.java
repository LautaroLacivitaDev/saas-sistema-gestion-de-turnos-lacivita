package com.lacivita.turnos.notifications.web;

import com.lacivita.turnos.notifications.application.DemoInbox;
import com.lacivita.turnos.notifications.application.DemoInbox.DemoEmailView;
import com.lacivita.turnos.notifications.domain.DemoEmailNotFoundException;
import com.lacivita.turnos.shared.domain.Email;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Buzón del modo demo. Fuera del modo demo no existe (responde 404). */
@Tag(name = "Demo")
@RestController
@RequestMapping("/api/public/demo/inbox")
@ConditionalOnBooleanProperty("app.demo.enabled")
class DemoInboxController {

    private static final MediaType CALENDAR = new MediaType("text", "calendar", StandardCharsets.UTF_8);

    private final DemoInbox inbox;

    DemoInboxController(DemoInbox inbox) {
        this.inbox = inbox;
    }

    @Operation(summary = "Últimos 20 emails de una dirección", description = "Solo en el modo demo.")
    @GetMapping
    List<DemoEmailView> inbox(@RequestParam String email) {
        return inbox.inboxOf(new Email(email));
    }

    @Operation(summary = "Evento de calendario de un email", description = "Solo en el modo demo.")
    @GetMapping("/{emailId}/turno.ics")
    ResponseEntity<String> calendar(@PathVariable UUID emailId, @RequestParam String email) {
        String calendar = inbox.calendarOf(emailId, new Email(email)).orElseThrow(DemoEmailNotFoundException::new);
        return ResponseEntity.ok()
                .contentType(CALENDAR)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("turno.ics")
                                .build()
                                .toString())
                .body(calendar);
    }
}
