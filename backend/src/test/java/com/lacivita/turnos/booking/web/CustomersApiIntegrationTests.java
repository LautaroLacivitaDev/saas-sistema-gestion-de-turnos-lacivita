package com.lacivita.turnos.booking.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.lacivita.turnos.ApiClient;
import com.lacivita.turnos.ApiClient.Session;
import com.lacivita.turnos.IntegrationTest;
import com.lacivita.turnos.RecordingMailer;
import java.time.Instant;
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

/** Base de clientes del panel: búsqueda, ficha con historial, notas y permisos. */
@IntegrationTest
class CustomersApiIntegrationTests {

    static final ZoneId BUENOS_AIRES = ZoneId.of("America/Argentina/Buenos_Aires");

    @Autowired
    MockMvcTester mvc;

    @Autowired
    RecordingMailer mailer;

    ApiClient api;
    Session owner;
    Session manager;
    Session barber;
    UUID businessId;
    UUID centro;
    UUID corte;
    String ana;

    @BeforeEach
    void setUp() {
        api = new ApiClient(mvc);
        owner = api.registerNewUser("Dueña");
        businessId = api.createBusiness(
                owner, "clientes-" + UUID.randomUUID().toString().substring(0, 8));
        centro = api.createBranch(owner, businessId, "Centro");
        manager = api.joinTeam(owner, businessId, "MANAGER", centro, mailer, "Gerente");
        barber = api.joinTeam(owner, businessId, "BARBER", centro, mailer, "Barbera");
        corte = UUID.fromString(ApiClient.read(api.post(owner, business("/services"), """
                        {"name":"Corte","category":"Cortes","baseDurationMinutes":30,"basePrice":8000}"""), "$.id"));
        api.put(barber, business("/barbers/" + barber.userId() + "/services/" + corte), "{}");

        ana = book("Ana González", "ana-" + UUID.randomUUID() + "@example.com", "11 4567-8901", 1, "10:00");
        book("Ana González", null, "11 4567-8901", 2, "10:00");
        book("Bruno Díaz", "bruno-" + UUID.randomUUID() + "@example.com", null, 1, "11:00");
    }

    @Test
    void findsCustomersByPartOfTheNameWithoutAccentsAndByPhoneWithSpaces() {
        assertThat(api.get(manager, business("/customers?q=gonzal")))
                .hasStatusOk()
                .bodyJson()
                .extractingPath("$.items[0].name")
                .isEqualTo("Ana González");
        assertThat(api.get(manager, business("/customers?q=4567 89")))
                .bodyJson()
                .extractingPath("$.items[0].appointments")
                .isEqualTo(2);
        assertThat(api.get(manager, business("/customers")))
                .bodyJson()
                .extractingPath("$.totalItems")
                .isEqualTo(2);
    }

    @Test
    void likeWildcardsAreSearchedAsText() {
        assertThat(api.get(manager, business("/customers?q=%25")))
                .bodyJson()
                .extractingPath("$.totalItems")
                .isEqualTo(0);
    }

    @Test
    void theCardShowsTheHistoryNewestFirstAndTheTeamCanAnnotateIt() {
        var detail = api.get(owner, business("/customers/" + ana));
        assertThat(detail).hasStatusOk();
        assertThat(detail).bodyJson().extractingPath("$.history.length()").isEqualTo(2);
        assertThat(detail)
                .bodyJson()
                .extractingPath("$.history[0].startsAt")
                .isEqualTo(at(2, "10:00").toString());

        var updated = api.put(manager, business("/customers/" + ana), """
                {"contact":{"name":"Ana María González","phone":"11 5555-0000"},
                 "notes":"Avisar por WhatsApp","preferences":"Fade bajo"}""");

        assertThat(updated).hasStatusOk();
        assertThat(updated).bodyJson().extractingPath("$.name").isEqualTo("Ana María González");
        assertThat(updated).bodyJson().extractingPath("$.email").isNull();
        assertThat(updated).bodyJson().extractingPath("$.preferences").isEqualTo("Fade bajo");
    }

    @Test
    void twoCustomersCannotShareAnEmail() {
        String brunoId = ApiClient.read(api.get(manager, business("/customers?q=bruno")), "$.items[0].id");
        String anaEmail = ApiClient.read(api.get(manager, business("/customers/" + ana)), "$.email");

        assertThat(api.put(manager, business("/customers/" + brunoId), """
                        {"contact":{"name":"Bruno Díaz","email":"%s"}}""".formatted(anaEmail)))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("customer_email_taken");
    }

    @Test
    void barbersAndOtherBusinessesDoNotSeeTheCustomerBase() {
        var stranger = api.registerNewUser("Dueño de otro negocio");
        api.createBusiness(stranger, "otro-" + UUID.randomUUID().toString().substring(0, 8));

        assertThat(api.get(barber, business("/customers"))).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(api.get(stranger, business("/customers"))).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(api.get(stranger, business("/customers/" + ana))).hasStatus(HttpStatus.FORBIDDEN);
    }

    /** Turno cargado desde el local; devuelve el id del cliente. */
    private String book(String name, String email, String phone, int daysAhead, String time) {
        String contact = "\"name\":\"%s\"".formatted(name)
                + (email == null ? "" : ",\"email\":\"%s\"".formatted(email))
                + (phone == null ? "" : ",\"phone\":\"%s\"".formatted(phone));
        var result = api.post(manager, business("/appointments"), """
                {"branchId":"%s","barberId":"%s","serviceId":"%s","startsAt":"%s","customer":{%s}}""".formatted(
                        centro, barber.userId(), corte, at(daysAhead, time), contact));
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return ApiClient.read(result, "$.customer.id");
    }

    private Instant at(int daysAhead, String time) {
        return ZonedDateTime.of(LocalDate.now(BUENOS_AIRES).plusDays(daysAhead), LocalTime.parse(time), BUENOS_AIRES)
                .toInstant();
    }

    private String business(String path) {
        return "/api/businesses/" + businessId + path;
    }
}
