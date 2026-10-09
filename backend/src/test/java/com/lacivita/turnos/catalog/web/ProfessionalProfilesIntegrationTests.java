package com.lacivita.turnos.catalog.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.lacivita.turnos.ApiClient;
import com.lacivita.turnos.ApiClient.Session;
import com.lacivita.turnos.IntegrationTest;
import com.lacivita.turnos.RecordingMailer;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

/** Perfil público de los profesionales: quién lo edita y qué muestra la página del negocio. */
@IntegrationTest
class ProfessionalProfilesIntegrationTests {

    static final String PROFILE = """
            {"bio":"Diez años cortando en el barrio.","specialties":["Fade","Barba"],
             "photoUrl":"https://example.com/barbera.jpg"}""";

    @Autowired
    MockMvcTester mvc;

    @Autowired
    RecordingMailer mailer;

    ApiClient api;
    Session owner;
    Session manager;
    Session barber;
    Session other;
    UUID businessId;
    String slug;
    UUID centro;

    @BeforeEach
    void setUp() {
        api = new ApiClient(mvc);
        owner = api.registerNewUser("Dueña");
        slug = "perfiles-" + UUID.randomUUID().toString().substring(0, 8);
        businessId = api.createBusiness(owner, slug);
        centro = api.createBranch(owner, businessId, "Centro");
        manager = api.joinTeam(owner, businessId, "MANAGER", centro, mailer, "Gerente");
        barber = api.joinTeam(owner, businessId, "BARBER", centro, mailer, "Barbera");
        other = api.joinTeam(owner, businessId, "BARBER", centro, mailer, "Barbero");
        var corte = ApiClient.read(api.post(owner, business("/services"), """
                        {"name":"Corte","category":"Cortes","baseDurationMinutes":30,"basePrice":8000}"""), "$.id");
        api.put(barber, business("/barbers/" + barber.userId() + "/services/" + corte), """
                {"price":9000}""");
    }

    @Test
    void theProfessionalWritesTheirProfileAndThePublicPageShowsIt() {
        assertThat(api.put(barber, profile(barber), PROFILE)).hasStatusOk();

        var page = api.get(null, "/api/public/businesses/" + slug + "/professionals");

        assertThat(page).hasStatusOk();
        assertThat(page).bodyJson().extractingPath("$.length()").isEqualTo(1);
        assertThat(page).bodyJson().extractingPath("$[0].name").isEqualTo("Barbera");
        assertThat(page).bodyJson().extractingPath("$[0].bio").isEqualTo("Diez años cortando en el barrio.");
        assertThat(page).bodyJson().extractingPath("$[0].specialties[1]").isEqualTo("Barba");
        assertThat(page).bodyJson().extractingPath("$[0].branchIds[0]").isEqualTo(centro.toString());
        assertThat(page).bodyJson().extractingPath("$[0].services[0].name").isEqualTo("Corte");
        assertThat(api.get(null, "/api/public/businesses/" + slug + "/professionals/" + barber.userId()))
                .bodyJson()
                .extractingPath("$.photoUrl")
                .isEqualTo("https://example.com/barbera.jpg");
    }

    @Test
    void whoeverDoesNotOfferServicesIsNotListed() {
        assertThat(api.get(null, "/api/public/businesses/" + slug + "/professionals/" + other.userId()))
                .hasStatus(HttpStatus.NOT_FOUND);
    }

    @Test
    void onlyTheProfessionalTheirManagerOrTheOwnerEditTheProfile() {
        assertThat(api.put(other, profile(barber), PROFILE)).hasStatus(HttpStatus.FORBIDDEN);
        assertThat(api.put(manager, profile(barber), PROFILE)).hasStatusOk();
        assertThat(api.put(owner, profile(barber), PROFILE)).hasStatusOk();
    }

    @Test
    void thePhotoMustBeASecureLinkAndSpecialtiesAreLimited() {
        assertThat(api.put(barber, profile(barber), """
                        {"photoUrl":"http://example.com/foto.jpg"}"""))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("invalid_photo_url");
        assertThat(api.put(barber, profile(barber), """
                        {"specialties":["Fade","fade"]}""")).hasStatus(HttpStatus.BAD_REQUEST);
    }

    private String profile(Session professional) {
        return business("/barbers/" + professional.userId() + "/profile");
    }

    private String business(String path) {
        return "/api/businesses/" + businessId + path;
    }
}
