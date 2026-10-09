package com.lacivita.turnos.schedule.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.lacivita.turnos.ApiClient;
import com.lacivita.turnos.ApiClient.Session;
import com.lacivita.turnos.IntegrationTest;
import com.lacivita.turnos.RecordingMailer;
import java.io.UnsupportedEncodingException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
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
 * Agenda de punta a punta: un negocio con dos sucursales, un gerente en Centro, una barbera que trabaja
 * en Centro y Norte y un barbero solo en Norte. Hay un corte de 30 minutos que hace la barbera.
 */
@IntegrationTest
class ScheduleApiIntegrationTests {

    static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");

    @Autowired
    MockMvcTester mvc;

    @Autowired
    RecordingMailer mailer;

    ApiClient api;
    Session owner;
    Session manager;
    Session barber;
    Session barberNorte;
    UUID businessId;
    String slug;
    UUID centro;
    UUID norte;
    UUID corte;
    /** El próximo lunes, al menos mañana. */
    LocalDate monday;

    @BeforeEach
    void setUp() {
        api = new ApiClient(mvc);
        owner = api.registerNewUser("Dueña");
        slug = "agenda-" + UUID.randomUUID().toString().substring(0, 8);
        businessId = api.createBusiness(owner, slug);
        centro = api.createBranch(owner, businessId, "Centro");
        norte = api.createBranch(owner, businessId, "Norte");
        manager = api.joinTeam(owner, businessId, "MANAGER", centro, mailer, "Gerente");
        barber = api.joinTeam(owner, businessId, "BARBER", centro, mailer, "Barbera");
        api.put(owner, "/api/businesses/" + businessId + "/members/" + barber.userId() + "/branches", """
                {"branchIds":["%s","%s"]}""".formatted(
                        centro, norte));
        barberNorte = api.joinTeam(owner, businessId, "BARBER", norte, mailer, "Barbero Norte");

        var service = api.post(owner, "/api/businesses/" + businessId + "/services", """
                {"name":"Corte","category":"Cortes","baseDurationMinutes":30,"basePrice":8000}""");
        corte = UUID.fromString(ApiClient.read(service, "$.id"));
        api.put(barber, "/api/businesses/" + businessId + "/barbers/" + barber.userId() + "/services/" + corte, "{}");

        // Sin anticipación mínima, para que el resultado no dependa de la hora en que corre la prueba.
        api.put(owner, "/api/businesses/" + businessId + "/schedule-rules", """
                {"bufferMinutes":0,"minNoticeMinutes":0,"maxAdvanceDays":60,"slotStepMinutes":15}""");
        monday = LocalDate.now(BUENOS_AIRES).plusDays(1).with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
    }

    @Nested
    class BranchHours {

        @Test
        void managersSetTheHoursOfTheirBranchesOnly() {
            assertThat(setBranchHours(manager, centro, mondays("09:00", "18:00")))
                    .hasStatusOk();
            assertThat(setBranchHours(manager, norte, mondays("09:00", "18:00")))
                    .hasStatus(HttpStatus.FORBIDDEN)
                    .bodyJson()
                    .extractingPath("$.code")
                    .isEqualTo("outside_your_branches");
            assertThat(setBranchHours(barber, centro, mondays("09:00", "18:00")))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }

        @Test
        void theHoursArePublic() {
            setBranchHours(owner, centro, mondays("09:00", "13:00", "14:00", "18:00"));

            assertThat(api.get(null, "/api/public/businesses/" + slug + "/branches/" + centro + "/hours"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.days[0].ranges[1].start")
                    .isEqualTo("14:00");
        }
    }

    @Nested
    class WorkHours {

        @Test
        void aBarberCannotWorkAtTheSameTimeInTwoBranches() {
            assertThat(setWorkHours(barber, barber, centro, mondays("09:00", "13:00")))
                    .hasStatusOk();

            assertThat(setWorkHours(barber, barber, norte, mondays("12:00", "15:00")))
                    .hasStatus(HttpStatus.CONFLICT)
                    .bodyJson()
                    .extractingPath("$.code")
                    .isEqualTo("schedule_overlap");
            assertThat(setWorkHours(barber, barber, norte, mondays("13:00", "18:00")))
                    .hasStatusOk();
            assertThat(api.get(manager, "/api/businesses/" + businessId + "/barbers/" + barber.userId() + "/schedule"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$")
                    .asArray()
                    .hasSize(2);
        }

        @Test
        void twoSimultaneousSavesThatOverlapLetOnlyOneThrough() throws Exception {
            var start = new CountDownLatch(1);
            Callable<Integer> inCentro = () -> {
                start.await();
                return status(setWorkHours(owner, barber, centro, mondays("09:00", "13:00")));
            };
            Callable<Integer> inNorte = () -> {
                start.await();
                return status(setWorkHours(owner, barber, norte, mondays("10:00", "14:00")));
            };

            List<Integer> statuses;
            try (var executor = Executors.newFixedThreadPool(2)) {
                var first = executor.submit(inCentro);
                var second = executor.submit(inNorte);
                start.countDown();
                statuses = List.of(first.get(), second.get());
            }

            assertThat(statuses).containsExactlyInAnyOrder(200, 409);
        }

        @Test
        void hoursOnlyGoWhereThePersonWorks() {
            assertThat(setWorkHours(owner, barberNorte, centro, mondays("09:00", "13:00")))
                    .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT)
                    .bodyJson()
                    .extractingPath("$.code")
                    .isEqualTo("barber_not_in_branch");
        }

        @Test
        void aManagerSetsTheHoursOfBarbersOfTheirBranchesOnly() {
            assertThat(setWorkHours(manager, barber, centro, mondays("09:00", "13:00")))
                    .hasStatusOk();
            assertThat(setWorkHours(manager, barberNorte, norte, mondays("09:00", "13:00")))
                    .hasStatus(HttpStatus.FORBIDDEN);
            assertThat(setWorkHours(barberNorte, barber, centro, mondays("09:00", "13:00")))
                    .hasStatus(HttpStatus.FORBIDDEN)
                    .bodyJson()
                    .extractingPath("$.code")
                    .isEqualTo("only_own_schedule");
        }
    }

    @Nested
    class Availability {

        @BeforeEach
        void centroOpensOnMondays() {
            setBranchHours(owner, centro, mondays("09:00", "18:00"));
            setWorkHours(barber, barber, centro, mondays("09:00", "13:00", "14:00", "18:00"));
        }

        @Test
        void slotsFollowTheHoursAndSkipTheBreakWithoutASession() {
            var availability = availability(null);

            assertThat(availability).hasStatusOk();
            assertThat(localTimes(availability))
                    .startsWith("09:00", "09:15")
                    .contains("12:30", "14:00", "17:30")
                    .doesNotContain("12:45", "13:00", "13:30", "17:45");
            assertThat(availability)
                    .bodyJson()
                    .extractingPath("$.slots[0].barbers[0].barberName")
                    .isEqualTo("Barbera");
        }

        @Test
        void blocksTakeTimeAwayAndHolidaysTheWholeDay() {
            api.post(
                    barber,
                    "/api/businesses/" + businessId + "/barbers/" + barber.userId() + "/time-blocks",
                    """
                    {"startsAt":"%s","endsAt":"%s","reason":"Trámite"}""".formatted(at(monday, "10:00"), at(monday, "11:00")));

            assertThat(localTimes(availability(null)))
                    .contains("09:30", "11:00")
                    .doesNotContain("09:45", "10:00", "10:45");

            assertThat(api.post(manager, "/api/businesses/" + businessId + "/holidays", """
                            {"branchId":"%s","date":"%s","name":"Feriado local"}""".formatted(centro, monday)))
                    .hasStatus(HttpStatus.CREATED);
            assertThat(localTimes(availability(null))).isEmpty();
        }

        @Test
        void anyAvailableListsEveryBarberFreeAtEachTime() {
            var second = api.joinTeam(owner, businessId, "BARBER", centro, mailer, "Ana");
            api.put(
                    second,
                    "/api/businesses/" + businessId + "/barbers/" + second.userId() + "/services/" + corte,
                    """
                    {"price":9000,"durationMinutes":45}""");
            setWorkHours(second, second, centro, mondays("09:00", "11:00"));

            var availability = availability(null);

            assertThat(availability)
                    .bodyJson()
                    .extractingPath("$.slots[0].barbers[*].barberName")
                    .asArray()
                    .containsExactly("Ana", "Barbera");
            assertThat(availability)
                    .bodyJson()
                    .extractingPath("$.slots[0].barbers[0].durationMinutes")
                    .isEqualTo(45);
            assertThat(availability(second.userId()))
                    .bodyJson()
                    .extractingPath("$.slots[*].localTime")
                    .asArray()
                    .endsWith("10:15");
        }

        @Test
        void aComboTakesTheSumOfTheDurationsOfTheBarber() {
            var barba = api.post(owner, "/api/businesses/" + businessId + "/services", """
                    {"name":"Barba","category":"Barba","baseDurationMinutes":20,"basePrice":5000}""");
            var barbaId = ApiClient.read(barba, "$.id");
            api.put(
                    barber,
                    "/api/businesses/" + businessId + "/barbers/" + barber.userId() + "/services/" + barbaId,
                    "{}");
            var combo = api.post(owner, "/api/businesses/" + businessId + "/combos", """
                    {"name":"Corte y barba","serviceIds":["%s","%s"]}""".formatted(corte, barbaId));

            var availability = api.get(
                    null,
                    "/api/public/businesses/%s/availability?branchId=%s&date=%s&comboId=%s"
                            .formatted(slug, centro, monday, ApiClient.read(combo, "$.id")));

            assertThat(availability)
                    .bodyJson()
                    .extractingPath("$.slots[0].barbers[0].durationMinutes")
                    .isEqualTo(50);
            // 50 minutos: el último turno de la mañana empieza 12:00 y el de la tarde, 17:00.
            assertThat(localTimes(availability)).contains("12:00", "17:00").doesNotContain("12:15", "17:15");
        }

        @Test
        void someoneWhoLeavesTheTeamHasNoMoreSlots() {
            api.delete(owner, "/api/businesses/" + businessId + "/members/" + barber.userId());

            assertThat(localTimes(availability(null))).isEmpty();
        }
    }

    @Nested
    class Rules {

        @Test
        void onlyTheOwnerChangesTheRules() {
            String rules = """
                    {"bufferMinutes":10,"minNoticeMinutes":120,"maxAdvanceDays":30,"slotStepMinutes":30}""";

            assertThat(api.put(manager, rulesPath(), rules)).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(api.put(owner, rulesPath(), rules)).hasStatusOk();
            assertThat(api.get(barber, rulesPath()))
                    .bodyJson()
                    .extractingPath("$.slotStepMinutes")
                    .isEqualTo(30);
        }

        @Test
        void wholeBusinessHolidaysAreForTheOwner() {
            String holiday = """
                    {"date":"%s","name":"Navidad"}""".formatted(monday);

            assertThat(api.post(manager, "/api/businesses/" + businessId + "/holidays", holiday))
                    .hasStatus(HttpStatus.FORBIDDEN);
            assertThat(api.post(owner, "/api/businesses/" + businessId + "/holidays", holiday))
                    .hasStatus(HttpStatus.CREATED);
            assertThat(api.post(owner, "/api/businesses/" + businessId + "/holidays", holiday))
                    .hasStatus(HttpStatus.CONFLICT);
        }

        @Test
        void anotherBusinessCannotSeeTheSchedule() {
            var stranger = api.registerNewUser("Otra dueña");
            api.createBusiness(stranger, "otro-" + UUID.randomUUID().toString().substring(0, 8));

            assertThat(api.get(stranger, "/api/businesses/" + businessId + "/barbers/" + barber.userId() + "/schedule"))
                    .hasStatus(HttpStatus.FORBIDDEN);
            assertThat(setBranchHours(stranger, centro, mondays("09:00", "18:00")))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }

        private String rulesPath() {
            return "/api/businesses/" + businessId + "/schedule-rules";
        }
    }

    // --- Ayudantes ---

    private MvcTestResult setBranchHours(Session actor, UUID branch, String hours) {
        return api.put(actor, "/api/businesses/" + businessId + "/branches/" + branch + "/hours", hours);
    }

    private MvcTestResult setWorkHours(Session actor, Session of, UUID branch, String hours) {
        return api.put(
                actor, "/api/businesses/" + businessId + "/barbers/" + of.userId() + "/schedule/" + branch, hours);
    }

    private MvcTestResult availability(UUID barberId) {
        String uri = "/api/public/businesses/%s/availability?branchId=%s&date=%s&serviceId=%s"
                .formatted(slug, centro, monday, corte);
        return api.get(null, barberId == null ? uri : uri + "&barberId=" + barberId);
    }

    /** Horario semanal con franjas solo los lunes: pares de inicio y fin. */
    private static String mondays(String... times) {
        var ranges = new StringBuilder();
        for (int i = 0; i < times.length; i += 2) {
            ranges.append(i == 0 ? "" : ",")
                    .append("{\"start\":\"%s\",\"end\":\"%s\"}".formatted(times[i], times[i + 1]));
        }
        return "{\"days\":[{\"day\":\"MONDAY\",\"ranges\":[" + ranges + "]}]}";
    }

    private static String at(LocalDate date, String time) {
        return ZonedDateTime.of(date, LocalTime.parse(time), BUENOS_AIRES)
                .toInstant()
                .toString();
    }

    private static List<String> localTimes(MvcTestResult availability) {
        List<String> times = JsonPath.read(content(availability), "$.slots[*].localTime");
        return times;
    }

    private static String content(MvcTestResult result) {
        try {
            return result.getResponse().getContentAsString();
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private static int status(MvcTestResult result) {
        return result.getResponse().getStatus();
    }
}
