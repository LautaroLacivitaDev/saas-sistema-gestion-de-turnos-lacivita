package com.lacivita.turnos.notifications.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.lacivita.turnos.ApiClient;
import com.lacivita.turnos.SpaCsrf;
import com.lacivita.turnos.TestcontainersConfiguration;
import com.lacivita.turnos.notifications.domain.NotificationChannel;
import com.lacivita.turnos.notifications.domain.OutgoingMessage;
import com.lacivita.turnos.notifications.domain.OutgoingMessage.Attachment;
import com.lacivita.turnos.shared.mail.Mailer;
import java.nio.charset.StandardCharsets;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/**
 * Modo demo (perfil "demo"): los emails no salen, quedan en el buzón de la demo, y Flyway carga el negocio de
 * ejemplo. Contexto propio (sin los dobles de prueba de {@code @IntegrationTest}) para probar las
 * implementaciones reales del modo demo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "demo"})
@Import(TestcontainersConfiguration.class)
class DemoInboxIntegrationTests {

    static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");
    static final String PALERMO = "d0000000-0000-7000-8000-000000000011";
    static final String CORTE = "d0000000-0000-7000-8000-000000000301";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    Mailer mailer;

    @Autowired
    Flyway flyway;

    @Autowired
    List<NotificationChannel> channels;

    @Test
    void demoModeReplacesTheRealSenders() {
        assertThat(mailer.getClass().getSimpleName()).isEqualTo("DemoMailer");
        assertThat(channels)
                .extracting(channel -> channel.getClass().getSimpleName())
                .containsExactly("DemoEmailChannel");
    }

    @Test
    void accountEmailsLandInTheInboxOfTheirAddress() {
        var person = new ApiClient(mvc).registerNewUser("Visitante");

        assertThat(mvc.get().uri("/api/public/demo/inbox").param("email", person.email()))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[0].text")
                .asString()
                .contains("/verificar-email?token=");
        assertThat(mvc.get().uri("/api/public/demo/inbox").param("email", "otra-" + person.email()))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.length()")
                .isEqualTo(0);
    }

    @Test
    void appointmentEmailsKeepTheirHtmlAndCalendarInvite() {
        String address = "demo-" + UUID.randomUUID() + "@example.com";
        String ics = "BEGIN:VCALENDAR\r\nEND:VCALENDAR\r\n";
        channels.getFirst()
                .send(new OutgoingMessage(
                        address,
                        "Tu turno está reservado",
                        "Te esperamos",
                        "<p>Te esperamos</p>",
                        List.of(new Attachment(
                                "turno.ics", "text/calendar; method=PUBLISH", ics.getBytes(StandardCharsets.UTF_8)))));

        var inbox =
                mvc.get().uri("/api/public/demo/inbox").param("email", address).exchange();
        assertThat(inbox).hasStatusOk().bodyJson().extractingPath("$[0].html").isEqualTo("<p>Te esperamos</p>");
        assertThat(inbox).bodyJson().extractingPath("$[0].hasCalendar").isEqualTo(true);
        String emailId = ApiClient.read(inbox, "$[0].id");

        assertThat(mvc.get()
                        .uri("/api/public/demo/inbox/{id}/turno.ics", emailId)
                        .param("email", address))
                .hasStatusOk()
                .hasBodyTextEqualTo(ics);
        assertThat(mvc.get()
                        .uri("/api/public/demo/inbox/{id}/turno.ics", emailId)
                        .param("email", "x" + address))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void theSampleBusinessIsPublishedWithItsTeam() {
        assertThat(mvc.get().uri("/api/public/businesses/barberia-el-faro/professionals"))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$[*].name")
                .asArray()
                .containsExactlyInAnyOrder("Nico Ríos", "Juli Sosa", "Sofi Lagos");
    }

    @Test
    void theSampleDataIsResetOnEveryStartup() {
        var result = flyway.migrate();

        assertThat(result.migrations)
                .extracting(migration -> migration.description)
                .contains("demo data");
        theSampleBusinessIsPublishedWithItsTeam();
    }

    @Test
    void theSampleBusinessHasFreeTimesNextWeek() {
        var nextTuesday = LocalDate.now(BUENOS_AIRES).with(TemporalAdjusters.next(DayOfWeek.TUESDAY));
        assertThat(mvc.get()
                        .uri("/api/public/businesses/barberia-el-faro/availability")
                        .param("branchId", PALERMO)
                        .param("serviceId", CORTE)
                        .param("date", nextTuesday.toString()))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.slots.length()")
                .asNumber()
                .satisfies(count -> assertThat(count.intValue()).isPositive());
    }

    @Test
    void theSampleAccountsCanSignInWithThePublishedPassword() {
        assertThat(mvc.post()
                        .uri("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"duena.demo@example.com","password":"demo1234"}""")
                        .with(SpaCsrf.spaCsrf()))
                .hasStatusOk();
    }

    @Test
    void anInvalidAddressIsRejected() {
        assertThat(mvc.get().uri("/api/public/demo/inbox").param("email", "no-es-un-email"))
                .hasStatus(HttpStatus.BAD_REQUEST);
    }
}
