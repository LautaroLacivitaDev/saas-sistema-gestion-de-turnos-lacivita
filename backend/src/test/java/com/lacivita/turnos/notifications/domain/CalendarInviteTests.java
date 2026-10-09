package com.lacivita.turnos.notifications.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.lacivita.turnos.booking.AppointmentDetails;
import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.shared.domain.Money;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CalendarInviteTests {

    static final UUID ID = UUID.fromString("0192a3b4-c5d6-7e8f-9a0b-1c2d3e4f5a6b");
    static final Instant NOW = Instant.parse("2026-10-09T12:00:00Z");

    @Test
    void theEventHasTheTimesInUtcAndAnIdThatStaysTheSameForTheAppointment() {
        String ics = ics(appointment("CONFIRMED", "Centro", 3));

        assertThat(ics)
                .startsWith("BEGIN:VCALENDAR\r\n")
                .contains("METHOD:PUBLISH\r\n")
                .contains("UID:" + ID + "@laciturnos\r\n")
                .contains("SEQUENCE:3\r\n")
                .contains("DTSTART:20261012T130000Z\r\n")
                .contains("DTEND:20261012T133000Z\r\n")
                .contains("STATUS:CONFIRMED\r\n")
                .endsWith("END:VCALENDAR\r\n");
    }

    @Test
    void aCancelledAppointmentReplacesTheEventAsCancelled() {
        assertThat(ics(appointment("CANCELLED", "Centro", 4))).contains("STATUS:CANCELLED\r\n");
        assertThat(ics(appointment("PENDING", "Centro", 0))).contains("STATUS:TENTATIVE\r\n");
    }

    @Test
    void textIsEscapedAndLongLinesAreFoldedWithoutBreakingAccents() {
        String branch = "Sucursal Ñuñoa; esquina, con acentos á é í ó ú ".repeat(4);

        String ics = ics(appointment("CONFIRMED", branch, 1));

        assertThat(ics).contains("Sucursal Ñuñoa\\; esquina\\, con");
        for (String line : ics.split("\r\n")) {
            assertThat(line.getBytes(StandardCharsets.UTF_8).length).isLessThanOrEqualTo(75);
        }
        String unfolded = ics.replace("\r\n ", "");
        assertThat(unfolded).contains("LOCATION:" + CalendarInvite.escape(branch + ", Av. Siempre Viva 742"));
    }

    private static String ics(AppointmentDetails appointment) {
        return new String(CalendarInvite.of(appointment, NOW).content(), StandardCharsets.UTF_8);
    }

    private static AppointmentDetails appointment(String status, String branchName, long revision) {
        return new AppointmentDetails(
                ID,
                UUID.randomUUID(),
                "Barbería Sur",
                UUID.randomUUID(),
                branchName,
                "Av. Siempre Viva 742",
                ZoneId.of("America/Argentina/Buenos_Aires"),
                UUID.randomUUID(),
                "Juan",
                "Ana",
                new Email("ana@example.com"),
                status,
                Instant.parse("2026-10-12T13:00:00Z"),
                Instant.parse("2026-10-12T13:30:00Z"),
                List.of("Corte"),
                Money.of("9000"),
                revision);
    }
}
