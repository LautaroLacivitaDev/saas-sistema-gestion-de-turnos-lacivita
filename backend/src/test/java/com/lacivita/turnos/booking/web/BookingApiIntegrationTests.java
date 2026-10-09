package com.lacivita.turnos.booking.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.lacivita.turnos.ApiClient;
import com.lacivita.turnos.ApiClient.Session;
import com.lacivita.turnos.IntegrationTest;
import com.lacivita.turnos.RecordingMailer;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Reservas de punta a punta. Centro atiende todos los días de 9 a 18; la barbera y el barbero trabajan
 * ahí de 9 a 13. Hay un corte de 30 minutos: la barbera lo cobra $9000 y el barbero, el precio base de
 * $8000. Los turnos son de mañana, para que no dependan del día de la semana.
 */
@IntegrationTest
class BookingApiIntegrationTests {

    static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");

    @Autowired
    MockMvcTester mvc;

    @Autowired
    RecordingMailer mailer;

    ApiClient api;
    Session owner;
    Session manager;
    Session barber;
    Session other;
    Session barberNorte;
    UUID businessId;
    String slug;
    UUID centro;
    UUID corte;
    LocalDate tomorrow;

    @BeforeEach
    void setUp() {
        api = new ApiClient(mvc);
        owner = api.registerNewUser("Dueña");
        slug = "reservas-" + UUID.randomUUID().toString().substring(0, 8);
        businessId = api.createBusiness(owner, slug);
        centro = api.createBranch(owner, businessId, "Centro");
        var norte = api.createBranch(owner, businessId, "Norte");
        manager = api.joinTeam(owner, businessId, "MANAGER", centro, mailer, "Gerente");
        barber = api.joinTeam(owner, businessId, "BARBER", centro, mailer, "Barbera");
        other = api.joinTeam(owner, businessId, "BARBER", centro, mailer, "Barbero");
        barberNorte = api.joinTeam(owner, businessId, "BARBER", norte, mailer, "Barbero Norte");

        corte = UUID.fromString(ApiClient.read(api.post(owner, business("/services"), """
                        {"name":"Corte","category":"Cortes","baseDurationMinutes":30,"basePrice":8000}"""), "$.id"));
        api.put(barber, business("/barbers/" + barber.userId() + "/services/" + corte), """
                {"price":9000}""");
        api.put(other, business("/barbers/" + other.userId() + "/services/" + corte), "{}");

        api.put(owner, business("/schedule-rules"), """
                {"bufferMinutes":0,"minNoticeMinutes":0,"maxAdvanceDays":60,"slotStepMinutes":15}""");
        api.put(owner, business("/branches/" + centro + "/hours"), everyDay("09:00", "18:00"));
        api.put(barber, business("/barbers/" + barber.userId() + "/schedule/" + centro), everyDay("09:00", "13:00"));
        api.put(other, business("/barbers/" + other.userId() + "/schedule/" + centro), everyDay("09:00", "13:00"));
        tomorrow = LocalDate.now(BUENOS_AIRES).plusDays(1);
    }

    @Nested
    class OnlineBooking {

        @Test
        void aGuestBooksWithTheCodeFromTheEmailAndGetsALinkToManageIt() {
            var hold = hold(barber.userId(), "10:00");
            assertThat(hold).hasStatus(HttpStatus.CREATED);
            assertAmount(hold, "$.totalPrice", "9000");
            var holdId = ApiClient.read(hold, "$.holdId");

            String email = "invitada-" + UUID.randomUUID() + "@example.com";
            assertThat(requestCode(holdId, email)).hasStatus(HttpStatus.ACCEPTED);
            var confirmed = confirm(holdId, mailer.lastCodeSentTo(email).orElseThrow(), null);

            assertThat(confirmed).hasStatus(HttpStatus.CREATED);
            assertThat(confirmed).bodyJson().extractingPath("$.status").isEqualTo("CONFIRMED");
            assertThat(confirmed)
                    .bodyJson()
                    .extractingPath("$.lines[0].serviceName")
                    .isEqualTo("Corte");
            assertThat(localTimes(availability(barber.userId())))
                    .contains("09:30", "10:30")
                    .doesNotContain("10:00");

            String token = mailer.lastTokenSentTo(email).orElseThrow();
            assertThat(api.post(null, "/api/public/appointments/lookup", tokenJson(token)))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.appointment.customer.email")
                    .isEqualTo(email);
        }

        @Test
        void aWrongCodeIsRejectedAndAfterFiveTheRightOneNoLongerWorks() {
            var holdId = ApiClient.read(hold(barber.userId(), "10:00"), "$.holdId");
            String email = "invitado-" + UUID.randomUUID() + "@example.com";
            requestCode(holdId, email);
            String code = mailer.lastCodeSentTo(email).orElseThrow();
            String wrong = code.equals("000000") ? "111111" : "000000";

            for (int i = 0; i < 5; i++) {
                assertThat(confirm(holdId, wrong, null))
                        .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                        .bodyJson()
                        .extractingPath("$.code")
                        .isEqualTo("invalid_code");
            }
            assertThat(confirm(holdId, code, null)).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        }

        @Test
        void someoneWithAVerifiedEmailConfirmsWithoutACode() {
            var client = api.registerNewUser("Clienta");
            String verification = mailer.lastTokenSentTo(client.email()).orElseThrow();
            api.post(client, "/api/auth/email-verification", tokenJson(verification));
            var holdId = ApiClient.read(hold(barber.userId(), "11:00"), "$.holdId");

            assertThat(confirm(holdId, null, client)).hasStatus(HttpStatus.CREATED);
        }

        @Test
        void aHeldTimeIsNotOfferedAndCannotBeTakenTwice() {
            assertThat(hold(barber.userId(), "10:00")).hasStatus(HttpStatus.CREATED);

            assertThat(localTimes(availability(barber.userId()))).doesNotContain("10:00");
            assertThat(hold(barber.userId(), "10:00"))
                    .hasStatus(HttpStatus.CONFLICT)
                    .bodyJson()
                    .extractingPath("$.code")
                    .isEqualTo("slot_not_available");
        }

        @Test
        void twoSimultaneousBookingsOfTheSameTimeLetOnlyOneThrough() throws Exception {
            var start = new CountDownLatch(1);
            Callable<Integer> booking = () -> {
                start.await();
                return hold(barber.userId(), "12:00").getResponse().getStatus();
            };

            List<Integer> statuses;
            try (var executor = Executors.newFixedThreadPool(2)) {
                var first = executor.submit(booking);
                var second = executor.submit(booking);
                start.countDown();
                statuses = List.of(first.get(), second.get());
            }

            assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        }

        @Test
        void anyAvailableGoesToTheBarberWithFewerAppointmentsThatDay() {
            book(manager, barber, "11:00", true);

            var hold = hold(null, "09:00");

            assertThat(hold).bodyJson().extractingPath("$.barberName").isEqualTo("Barbero");
            assertAmount(hold, "$.totalPrice", "8000");
        }
    }

    @Nested
    class CustomerLink {

        String token;

        @BeforeEach
        void aGuestHasAnAppointmentAtTen() {
            var holdId = ApiClient.read(hold(barber.userId(), "10:00"), "$.holdId");
            String email = "cliente-" + UUID.randomUUID() + "@example.com";
            requestCode(holdId, email);
            confirm(holdId, mailer.lastCodeSentTo(email).orElseThrow(), null);
            token = mailer.lastTokenSentTo(email).orElseThrow();
        }

        @Test
        void theCustomerMovesTheAppointmentToAnotherFreeTime() {
            var moved = api.post(null, "/api/public/appointments/reschedule", """
                    {"token":"%s","startsAt":"%s"}""".formatted(token, at("11:30")));

            assertThat(moved).hasStatusOk();
            assertThat(localTimes(availability(barber.userId())))
                    .contains("10:00")
                    .doesNotContain("11:30");
        }

        @Test
        void theCustomerCancelsAndTheTimeIsFreeAgain() {
            assertThat(api.post(null, "/api/public/appointments/cancel", tokenJson(token)))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.appointment.status")
                    .isEqualTo("CANCELLED");
            assertThat(localTimes(availability(barber.userId()))).contains("10:00");
        }

        @Test
        void afterTheDeadlineTheCustomerCannotCancel() {
            api.put(owner, business("/booking-settings"), """
                    {"cancellationNoticeHours":48}""");

            assertThat(api.post(null, "/api/public/appointments/cancel", tokenJson(token)))
                    .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                    .bodyJson()
                    .extractingPath("$.code")
                    .isEqualTo("change_deadline_passed");
        }

        @Test
        void anUnknownLinkFindsNothing() {
            assertThat(api.post(null, "/api/public/appointments/lookup", tokenJson("no-existe")))
                    .hasStatus(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    class Agenda {

        @Test
        void theTeamLoadsAppointmentsAndTheDatabaseNeverLetsThemOverlap() {
            assertThat(book(barber, barber, "09:00", true)).hasStatus(HttpStatus.CREATED);

            assertThat(book(manager, barber, "09:15", true))
                    .hasStatus(HttpStatus.CONFLICT)
                    .bodyJson()
                    .extractingPath("$.code")
                    .isEqualTo("slot_not_available");
        }

        @Test
        void aCancelledAppointmentFreesItsTime() {
            var id = ApiClient.read(book(manager, barber, "09:00", true), "$.id");

            assertThat(status(manager, id, "COMPLETED"))
                    .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                    .bodyJson()
                    .extractingPath("$.code")
                    .isEqualTo("appointment_not_started");
            assertThat(status(manager, id, "CANCELLED")).hasStatusOk();
            assertThat(book(manager, barber, "09:00", true)).hasStatus(HttpStatus.CREATED);
        }

        @Test
        void thePriceIsTheOneAtBookingTime() {
            book(manager, barber, "09:00", true);
            api.put(barber, business("/barbers/" + barber.userId() + "/services/" + corte), """
                    {"price":12000}""");

            assertAmount(agenda(owner), "$[0].totalPrice", "9000");
        }

        @Test
        void eachOneSeesTheirBranchesAndOnlyTheirOwnCustomersContact() {
            book(manager, other, "09:00", true);

            assertThat(agenda(manager))
                    .bodyJson()
                    .extractingPath("$[0].customer.phone")
                    .isEqualTo("1145678901");
            assertThat(agenda(barber))
                    .bodyJson()
                    .extractingPath("$[0].customer")
                    .isNull();
            assertThat(agenda(other))
                    .bodyJson()
                    .extractingPath("$[0].customer.name")
                    .isEqualTo("Cliente de mostrador");
            assertThat(agenda(barberNorte))
                    .bodyJson()
                    .extractingPath("$")
                    .asArray()
                    .isEmpty();
        }

        @Test
        void aBarberOfAnotherBranchCannotLoadAppointmentsHere() {
            assertThat(book(barberNorte, barber, "09:00", true))
                    .hasStatus(HttpStatus.FORBIDDEN)
                    .bodyJson()
                    .extractingPath("$.code")
                    .isEqualTo("outside_your_branches");
        }

        @Test
        void anotherBusinessCannotSeeTheAgenda() {
            var stranger = api.registerNewUser("Otra dueña");
            api.createBusiness(stranger, "otro-" + UUID.randomUUID().toString().substring(0, 8));

            assertThat(agenda(stranger)).hasStatus(HttpStatus.FORBIDDEN);
        }

        @Test
        void onlyTheOwnerChangesTheCancellationDeadline() {
            String policy = """
                    {"cancellationNoticeHours":24}""";

            assertThat(api.put(manager, business("/booking-settings"), policy)).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(api.put(owner, business("/booking-settings"), policy)).hasStatusOk();
        }
    }

    // --- Ayudantes ---

    private MvcTestResult hold(UUID barberId, String time) {
        String chosen = barberId == null ? "" : ",\"barberId\":\"%s\"".formatted(barberId);
        return api.post(
                null, "/api/public/businesses/" + slug + "/holds", """
                {"branchId":"%s","serviceId":"%s","startsAt":"%s"%s}""".formatted(centro, corte, at(time), chosen));
    }

    private MvcTestResult requestCode(String holdId, String email) {
        return api.post(
                null, "/api/public/businesses/" + slug + "/holds/" + holdId + "/guest-code", """
                {"name":"Invitada","email":"%s","phone":"11 4567-8901","humanToken":"ok"}""".formatted(email));
    }

    private MvcTestResult confirm(String holdId, String code, Session session) {
        String body = code == null ? "{}" : "{\"code\":\"%s\"}".formatted(code);
        return api.post(session, "/api/public/businesses/" + slug + "/holds/" + holdId + "/confirm", body);
    }

    private MvcTestResult book(Session actor, Session with, String time, boolean confirmed) {
        return api.post(
                actor, business("/appointments"), """
                {"branchId":"%s","barberId":"%s","serviceId":"%s","startsAt":"%s","confirmed":%s,
                 "customer":{"name":"Cliente de mostrador","phone":"11 4567-8901"}}""".formatted(centro, with.userId(), corte, at(time), confirmed));
    }

    private MvcTestResult status(Session actor, String appointmentId, String status) {
        return api.put(actor, business("/appointments/" + appointmentId + "/status"), """
                {"status":"%s"}""".formatted(status));
    }

    private MvcTestResult agenda(Session actor) {
        var from = tomorrow.atStartOfDay(BUENOS_AIRES).toInstant();
        return api.get(actor, business("/appointments?from=%s&to=%s".formatted(from, from.plus(Duration.ofDays(1)))));
    }

    private MvcTestResult availability(UUID barberId) {
        return api.get(
                null,
                "/api/public/businesses/%s/availability?branchId=%s&date=%s&serviceId=%s&barberId=%s"
                        .formatted(slug, centro, tomorrow, corte, barberId));
    }

    private String business(String path) {
        return "/api/businesses/" + businessId + path;
    }

    private Instant at(String time) {
        return ZonedDateTime.of(tomorrow, LocalTime.parse(time), BUENOS_AIRES).toInstant();
    }

    private static String tokenJson(String token) {
        return "{\"token\":\"%s\"}".formatted(token);
    }

    /** Horario semanal igual todos los días. */
    private static String everyDay(String start, String end) {
        var days = new StringBuilder();
        for (var day : DayOfWeek.values()) {
            days.append(days.isEmpty() ? "" : ",")
                    .append("{\"day\":\"%s\",\"ranges\":[{\"start\":\"%s\",\"end\":\"%s\"}]}"
                            .formatted(day, start, end));
        }
        return "{\"days\":[" + days + "]}";
    }

    private static List<String> localTimes(MvcTestResult availability) {
        return JsonPath.read(content(availability), "$.slots[*].localTime");
    }

    private static void assertAmount(MvcTestResult result, String path, String expected) {
        Object value = JsonPath.read(content(result), path);
        assertThat(new BigDecimal(String.valueOf(value))).isEqualByComparingTo(expected);
    }

    private static String content(MvcTestResult result) {
        try {
            return result.getResponse().getContentAsString();
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
