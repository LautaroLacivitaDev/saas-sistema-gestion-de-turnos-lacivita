package com.lacivita.turnos.catalog.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.lacivita.turnos.ApiClient;
import com.lacivita.turnos.ApiClient.Session;
import com.lacivita.turnos.IntegrationTest;
import com.lacivita.turnos.RecordingMailer;
import com.lacivita.turnos.shared.tenancy.TenantContext;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

/**
 * Catálogo de punta a punta: un negocio con dos sucursales, un gerente y un barbero en Centro, y otro
 * barbero en Norte. El dueño ya cargó un corte de 30 minutos a $8000.
 */
@IntegrationTest
class CatalogApiIntegrationTests {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    RecordingMailer mailer;

    @Autowired
    JdbcClient jdbc;

    ApiClient api;
    Session owner;
    Session manager;
    Session barber;
    Session barberNorte;
    UUID businessId;
    String slug;
    UUID corte;

    @BeforeEach
    void setUp() {
        api = new ApiClient(mvc);
        owner = api.registerNewUser("Dueña");
        slug = "catalogo-" + UUID.randomUUID().toString().substring(0, 8);
        businessId = api.createBusiness(owner, slug);
        var centro = api.createBranch(owner, businessId, "Centro");
        var norte = api.createBranch(owner, businessId, "Norte");
        manager = api.joinTeam(owner, businessId, "MANAGER", centro, mailer, "Gerente");
        barber = api.joinTeam(owner, businessId, "BARBER", centro, mailer, "Barbero Centro");
        barberNorte = api.joinTeam(owner, businessId, "BARBER", norte, mailer, "Barbero Norte");
        corte = createService(owner, "Corte", "Cortes", 30, "8000");
    }

    @Nested
    class Services {

        @Test
        void managersAddServicesAndBarbersOnlySeeThem() {
            assertThat(api.post(manager, services(), serviceJson("Barba", "Barba", 20, "5000")))
                    .hasStatus(HttpStatus.CREATED);
            assertThat(api.post(barber, services(), serviceJson("Cejas", "Cejas", 15, "3000")))
                    .hasStatus(HttpStatus.FORBIDDEN);
            assertThat(api.get(barber, services()))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.totalItems")
                    .isEqualTo(2);
        }

        @Test
        void twoServicesCannotShareANameRegardlessOfCase() {
            assertThat(api.post(owner, services(), serviceJson("CORTE", "Cortes", 30, "8000")))
                    .hasStatus(HttpStatus.CONFLICT)
                    .bodyJson()
                    .extractingPath("$.code")
                    .isEqualTo("service_name_taken");
        }

        @Test
        void onlyTheOwnerSetsThePriceRange() {
            String range = """
                    {"min":7000,"max":10000}""";

            assertThat(api.put(manager, service(corte) + "/price-range", range)).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(api.put(owner, service(corte) + "/price-range", range)).hasStatusOk();
        }

        @Test
        void aBarberProposalIsApprovedByAManagerAndTheProposerOffersIt() {
            var proposal = api.post(
                    barber,
                    "/api/businesses/" + businessId + "/service-proposals",
                    serviceJson("Diseño de cejas", "Cejas", 15, "3000"));
            assertThat(proposal)
                    .hasStatus(HttpStatus.CREATED)
                    .bodyJson()
                    .extractingPath("$.status")
                    .isEqualTo("PROPOSED");
            var proposalId = UUID.fromString(ApiClient.read(proposal, "$.id"));

            assertThat(api.post(barber, service(proposalId) + "/approve", "{}")).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(api.post(manager, service(proposalId) + "/approve", "{}"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.status")
                    .isEqualTo("ACTIVE");
            assertThat(api.get(barber, offeringsOf(barber)))
                    .bodyJson()
                    .extractingPath("$[*].serviceName")
                    .asArray()
                    .containsExactly("Diseño de cejas");
        }
    }

    @Nested
    class Prices {

        @BeforeEach
        void corteHasARange() {
            api.put(owner, service(corte) + "/price-range", """
                    {"min":7000,"max":10000}""");
        }

        @Test
        void aBarberPriceOutsideTheRangeWaitsForAManager() {
            assertThat(setTerms(barber, barber, corte, "9000"))
                    .bodyJson()
                    .extractingPath("$.outcome")
                    .isEqualTo("APPLIED");

            var request = setTerms(barber, barber, corte, "12000");

            assertThat(request).bodyJson().extractingPath("$.outcome").isEqualTo("AWAITING_APPROVAL");
            assertAmount(request, "$.offering.price", "9000");
            var pending = api.get(manager, priceRequests());
            assertThat(pending).bodyJson().extractingPath("$.totalItems").isEqualTo(1);
            var offeringId = ApiClient.read(pending, "$.items[0].offeringId");

            var approved = api.post(manager, priceRequests() + "/" + offeringId + "/approve", "{}");

            assertThat(approved).hasStatusOk();
            assertAmount(approved, "$.price", "12000");
            assertThat(api.get(owner, "/api/businesses/" + businessId + "/audit-log"))
                    .bodyJson()
                    .extractingPath("$.items[*].action")
                    .asArray()
                    .contains("offering.price_requested", "offering.price_approved");
        }

        @Test
        void managersSetPricesOfTheBarbersOfTheirBranchesDirectly() {
            assertThat(setTerms(manager, barber, corte, "15000"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.outcome")
                    .isEqualTo("APPLIED");
            assertThat(setTerms(manager, barberNorte, corte, "9000"))
                    .hasStatus(HttpStatus.FORBIDDEN)
                    .bodyJson()
                    .extractingPath("$.code")
                    .isEqualTo("outside_your_branches");
        }

        @Test
        void aBarberCannotChangeSomeoneElsesPrices() {
            assertThat(setTerms(barber, barberNorte, corte, "9000"))
                    .hasStatus(HttpStatus.FORBIDDEN)
                    .bodyJson()
                    .extractingPath("$.code")
                    .isEqualTo("only_own_offerings");
        }

        @Test
        void aManagerOnlyReviewsRequestsOfTheirBranches() {
            var request = setTerms(barberNorte, barberNorte, corte, "12000");
            var offeringId = ApiClient.read(request, "$.offering.id");

            assertThat(api.post(manager, priceRequests() + "/" + offeringId + "/reject", "{}"))
                    .hasStatus(HttpStatus.FORBIDDEN);
            assertThat(api.post(owner, priceRequests() + "/" + offeringId + "/reject", "{}"))
                    .hasStatusOk();
        }
    }

    @Nested
    class PublicCatalog {

        UUID barba;

        @BeforeEach
        void barbersOfferServices() {
            barba = createService(owner, "Barba", "Barba", 20, "5000");
            createService(owner, "Brushing", "Peinados", 30, "6000");
            setTerms(barber, barber, corte, "9000");
            setTerms(barber, barber, barba, null);
            setTerms(barberNorte, barberNorte, corte, null);
            api.post(owner, "/api/businesses/" + businessId + "/combos", """
                    {"name":"Corte y barba","serviceIds":["%s","%s"]}""".formatted(corte, barba));
        }

        @Test
        void showsWhatCanBeBookedWithThePriceOfEachBarberWithoutASession() {
            var catalog = api.get(null, publicCatalog(slug));

            assertThat(catalog).hasStatusOk();
            // Brushing no aparece: nadie lo hace.
            assertThat(catalog)
                    .bodyJson()
                    .extractingPath("$.services[*].name")
                    .asArray()
                    .containsExactly("Barba", "Corte");
            assertAmount(catalog, "$.services[1].fromPrice", "8000");
            assertThat(catalog)
                    .bodyJson()
                    .extractingPath("$.services[1].barbers[*].barberName")
                    .asArray()
                    .containsExactly("Barbero Norte", "Barbero Centro");
            // El combo lo puede hacer solo quien hace los dos servicios: 9000 + 5000.
            assertThat(catalog)
                    .bodyJson()
                    .extractingPath("$.combos[0].barbers[*].barberName")
                    .asArray()
                    .containsExactly("Barbero Centro");
            assertAmount(catalog, "$.combos[0].fromPrice", "14000");
            assertThat(catalog)
                    .bodyJson()
                    .extractingPath("$.combos[0].barbers[0].durationMinutes")
                    .isEqualTo(50);
        }

        @Test
        void retiredServicesAndFormerMembersDisappear() {
            api.put(owner, service(barba) + "/active", """
                    {"active":false}""");
            api.delete(owner, "/api/businesses/" + businessId + "/members/" + barberNorte.userId());

            var catalog = api.get(null, publicCatalog(slug));

            assertThat(catalog)
                    .bodyJson()
                    .extractingPath("$.services[*].name")
                    .asArray()
                    .containsExactly("Corte");
            assertThat(catalog)
                    .bodyJson()
                    .extractingPath("$.services[0].barbers[*].barberName")
                    .asArray()
                    .containsExactly("Barbero Centro");
            assertThat(catalog).bodyJson().extractingPath("$.combos").asArray().isEmpty();
        }

        @Test
        void anOldLinkStillShowsTheCatalogAndAnUnknownOneIsNotFound() {
            api.put(owner, "/api/businesses/" + businessId + "/slug", """
                    {"slug":"nuevo-%s"}""".formatted(slug));

            assertThat(api.get(null, publicCatalog(slug))).hasStatusOk();
            assertThat(api.get(null, publicCatalog("no-existe-" + UUID.randomUUID())))
                    .hasStatus(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    class Isolation {

        @Test
        void anotherBusinessCannotSeeOrTouchTheCatalog() {
            var stranger = api.registerNewUser("Otra dueña");
            var otherBusiness = api.createBusiness(
                    stranger, "otro-" + UUID.randomUUID().toString().substring(0, 8));

            assertThat(api.get(stranger, services())).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(setTerms(stranger, barber, corte, "1")).hasStatus(HttpStatus.FORBIDDEN);
            // Row Level Security: ni con SQL a mano se ven los servicios desde otro negocio.
            long visible = TenantContext.callInBusiness(
                    otherBusiness,
                    () -> jdbc.sql("SELECT count(*) FROM service WHERE business_id = :id")
                            .param("id", businessId)
                            .query(Long.class)
                            .single());
            assertThat(visible).isZero();
        }
    }

    // --- Ayudantes ---

    private UUID createService(Session actor, String name, String category, int minutes, String price) {
        var result = api.post(actor, services(), serviceJson(name, category, minutes, price));
        assertThat(result).hasStatus(HttpStatus.CREATED);
        return UUID.fromString(ApiClient.read(result, "$.id"));
    }

    private MvcTestResult setTerms(Session actor, Session of, UUID serviceId, String price) {
        return api.put(actor, offeringsOf(of) + "/" + serviceId, price == null ? "{}" : """
                {"price":%s}""".formatted(price));
    }

    private static String serviceJson(String name, String category, int minutes, String price) {
        return """
                {"name":"%s","category":"%s","baseDurationMinutes":%d,"basePrice":%s}""".formatted(name, category, minutes, price);
    }

    private String services() {
        return "/api/businesses/" + businessId + "/services";
    }

    private String service(UUID serviceId) {
        return services() + "/" + serviceId;
    }

    private String offeringsOf(Session person) {
        return "/api/businesses/" + businessId + "/barbers/" + person.userId() + "/services";
    }

    private String priceRequests() {
        return "/api/businesses/" + businessId + "/price-requests";
    }

    private static String publicCatalog(String slug) {
        return "/api/public/businesses/" + slug + "/catalog";
    }

    /** Compara importes por valor: el JSON trae {@code 8000.00}. */
    private static void assertAmount(MvcTestResult result, String path, String expected) {
        try {
            Object value = JsonPath.read(result.getResponse().getContentAsString(), path);
            if (value instanceof List<?> list) {
                value = list.getFirst();
            }
            assertThat(new BigDecimal(String.valueOf(value))).isEqualByComparingTo(expected);
        } catch (UnsupportedEncodingException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
