package com.lacivita.turnos.booking.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.lacivita.turnos.ApiClient;
import com.lacivita.turnos.ApiClient.Session;
import com.lacivita.turnos.IntegrationTest;
import com.lacivita.turnos.RecordingMailer;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** "Mis turnos": el historial de una cuenta en todos los negocios donde reservó. */
@IntegrationTest
class AccountHistoryIntegrationTests {

    static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");

    @Autowired
    MockMvcTester mvc;

    @Autowired
    RecordingMailer mailer;

    ApiClient api;
    Session client;

    @BeforeEach
    void setUp() {
        api = new ApiClient(mvc);
        client = api.registerNewUser("Clienta");
        String verification = mailer.lastTokenSentTo(client.email()).orElseThrow();
        api.post(client, "/api/auth/email-verification", "{\"token\":\"%s\"}".formatted(verification));
    }

    @Test
    void aCustomerSeesTheirAppointmentsInEveryBusinessNewestFirst() {
        var first = new Shop("Barbería Sur");
        var second = new Shop("Barbería Norte");
        first.bookAs(client, 1, "10:00");
        second.bookAs(client, 2, "11:00");

        var history = api.get(client, "/api/me/appointments");

        assertThat(history).hasStatusOk();
        assertThat(history).bodyJson().extractingPath("$.length()").isEqualTo(2);
        assertThat(history).bodyJson().extractingPath("$[0].slug").isEqualTo(second.slug);
        assertThat(history).bodyJson().extractingPath("$[0].businessName").isEqualTo("Barbería Norte");
        assertThat(history).bodyJson().extractingPath("$[0].appointment.status").isEqualTo("CONFIRMED");
        assertThat(history)
                .bodyJson()
                .extractingPath("$[1].appointment.lines[0].serviceId")
                .isEqualTo(first.service.toString());
        assertThat(history)
                .bodyJson()
                .extractingPath("$[1].appointment.barberId")
                .isEqualTo(first.owner.userId().toString());
    }

    @Test
    void nobodyElseSeesThem() {
        new Shop("Barbería Este").bookAs(client, 1, "10:00");
        var stranger = api.registerNewUser("Otra persona");

        assertThat(api.get(stranger, "/api/me/appointments"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.length()")
                .isEqualTo(0);
        assertThat(api.get(null, "/api/me/appointments")).hasStatus(HttpStatus.UNAUTHORIZED);
    }

    /** Un negocio donde la dueña atiende todos los días de 9 a 18. */
    private final class Shop {

        final Session owner;
        final String slug;
        final UUID branch;
        final UUID service;

        Shop(String name) {
            owner = api.registerNewUser("Dueña");
            slug = "historial-" + UUID.randomUUID().toString().substring(0, 8);
            var created = api.post(owner, "/api/businesses", """
                    {"name":"%s","slug":"%s","category":"BARBERSHOP"}""".formatted(name, slug));
            String businessId = ApiClient.read(created, "$.id");
            branch = api.createBranch(owner, UUID.fromString(businessId), "Centro");
            service = UUID.fromString(
                    ApiClient.read(api.post(owner, "/api/businesses/" + businessId + "/services", """
                            {"name":"Corte","category":"Cortes","baseDurationMinutes":30,"basePrice":8000}"""), "$.id"));
            String base = "/api/businesses/" + businessId;
            api.put(owner, base + "/barbers/" + owner.userId() + "/services/" + service, "{}");
            api.put(owner, base + "/schedule-rules", """
                    {"bufferMinutes":0,"minNoticeMinutes":0,"maxAdvanceDays":60,"slotStepMinutes":15}""");
            api.put(owner, base + "/branches/" + branch + "/hours", everyDay());
            api.put(owner, base + "/barbers/" + owner.userId() + "/schedule/" + branch, everyDay());
        }

        /** Reserva online con la cuenta (email verificado, sin código). */
        void bookAs(Session session, int daysAhead, String time) {
            var start = ZonedDateTime.of(
                            LocalDate.now(BUENOS_AIRES).plusDays(daysAhead), LocalTime.parse(time), BUENOS_AIRES)
                    .toInstant();
            var hold = api.post(null, "/api/public/businesses/" + slug + "/holds", """
                    {"branchId":"%s","serviceId":"%s","startsAt":"%s","barberId":"%s"}""".formatted(
                            branch, service, start, owner.userId()));
            assertThat(hold).hasStatus(HttpStatus.CREATED);
            String holdId = ApiClient.read(hold, "$.holdId");
            assertThat(api.post(session, "/api/public/businesses/" + slug + "/holds/" + holdId + "/confirm", "{}"))
                    .hasStatus(HttpStatus.CREATED);
        }
    }

    private static String everyDay() {
        var days = new StringBuilder();
        for (var day : DayOfWeek.values()) {
            days.append(days.isEmpty() ? "" : ",")
                    .append("{\"day\":\"%s\",\"ranges\":[{\"start\":\"09:00\",\"end\":\"18:00\"}]}".formatted(day));
        }
        return "{\"days\":[" + days + "]}";
    }
}
