package com.lacivita.turnos.business.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.lacivita.turnos.ApiClient;
import com.lacivita.turnos.IntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;

@IntegrationTest
class BusinessApiIntegrationTests {

    @Autowired
    MockMvcTester mvc;

    ApiClient api;

    @BeforeEach
    void setUp() {
        api = new ApiClient(mvc);
    }

    @Nested
    class Registration {

        @Test
        void theCreatorBecomesTheOwner() {
            var owner = api.registerNewUser("Dueña");

            var businessId = api.createBusiness(owner, slug());

            assertThat(api.get(owner, "/api/memberships"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$[0].role")
                    .isEqualTo("OWNER");
            assertThat(ApiClient.read(api.get(owner, "/api/memberships"), "$[0].businessId"))
                    .isEqualTo(businessId.toString());
        }

        @Test
        void aSlugCannotBeUsedByTwoBusinesses() {
            var slug = slug();
            api.createBusiness(api.registerNewUser("Uno"), slug);

            var second = api.post(api.registerNewUser("Dos"), "/api/businesses", """
                    {"name":"Otro","slug":"%s","category":"BARBERSHOP"}""".formatted(slug));

            assertThat(second).hasStatus(HttpStatus.CONFLICT);
            assertThat(second).bodyJson().extractingPath("$.code").isEqualTo("slug_taken");
        }

        @Test
        void reservedAndMalformedSlugsAreRejected() {
            var owner = api.registerNewUser("Dueño");

            assertThat(api.post(owner, "/api/businesses", """
                            {"name":"X","slug":"admin","category":"BARBERSHOP"}"""))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .extractingPath("$.code")
                    .isEqualTo("reserved_slug");
            assertThat(api.post(owner, "/api/businesses", """
                            {"name":"X","slug":"con espacios","category":"BARBERSHOP"}""")).hasStatus(HttpStatus.BAD_REQUEST);
        }

        @Test
        void slugAvailabilityIsCheckedWhileTyping() {
            var owner = api.registerNewUser("Dueño");
            var taken = slug();
            api.createBusiness(owner, taken);

            assertThat(api.get(owner, "/api/businesses/slug-availability?slug=" + taken))
                    .bodyJson()
                    .extractingPath("$.available")
                    .isEqualTo(false);
            assertThat(api.get(owner, "/api/businesses/slug-availability?slug=" + slug()))
                    .bodyJson()
                    .extractingPath("$.available")
                    .isEqualTo(true);
            assertThat(api.get(owner, "/api/businesses/slug-availability?slug=api"))
                    .bodyJson()
                    .extractingPath("$.code")
                    .isEqualTo("reserved_slug");
        }
    }

    @Nested
    class PublicPage {

        @Test
        void isAvailableWithoutSessionAndListsBranches() {
            var owner = api.registerNewUser("Dueña");
            var slug = slug();
            var businessId = api.createBusiness(owner, slug);
            api.createBranch(owner, businessId, "Centro");

            var page = api.get(null, "/api/public/businesses/" + slug);

            assertThat(page).hasStatusOk();
            assertThat(page).bodyJson().extractingPath("$.canonicalSlug").isEqualTo(slug);
            assertThat(page).bodyJson().extractingPath("$.branches[0].name").isEqualTo("Centro");
            assertThat(page)
                    .bodyJson()
                    .extractingPath("$.branches[0].timeZone")
                    .isEqualTo("America/Argentina/Buenos_Aires");
        }

        @Test
        void anOldSlugStillFindsTheBusinessAndPointsToTheNewOne() {
            var owner = api.registerNewUser("Dueña");
            var oldSlug = slug();
            var newSlug = slug();
            var businessId = api.createBusiness(owner, oldSlug);

            assertThat(api.put(owner, "/api/businesses/" + businessId + "/slug", """
                            {"slug":"%s"}""".formatted(newSlug)))
                    .hasStatusOk();

            assertThat(api.get(null, "/api/public/businesses/" + oldSlug))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.canonicalSlug")
                    .isEqualTo(newSlug);
        }

        @Test
        void anOldSlugStaysReservedForItsBusiness() {
            var owner = api.registerNewUser("Dueña");
            var oldSlug = slug();
            var businessId = api.createBusiness(owner, oldSlug);
            api.put(owner, "/api/businesses/" + businessId + "/slug", """
                    {"slug":"%s"}""".formatted(slug()));

            var other = api.post(api.registerNewUser("Otro"), "/api/businesses", """
                    {"name":"Otro","slug":"%s","category":"BARBERSHOP"}""".formatted(oldSlug));

            assertThat(other).hasStatus(HttpStatus.CONFLICT);
        }

        @Test
        void unknownSlugsAreNotFound() {
            assertThat(api.get(null, "/api/public/businesses/no-existe-" + UUID.randomUUID()))
                    .hasStatus(HttpStatus.NOT_FOUND);
        }
    }

    @Nested
    class Permissions {

        @Test
        void onlyTheOwnerEditsTheBusinessAndItsBranches() {
            var owner = api.registerNewUser("Dueña");
            var stranger = api.registerNewUser("Ajena");
            var businessId = api.createBusiness(owner, slug());
            String profile = """
                    {"name":"Nuevo nombre","category":"BEAUTY_SALON","searchable":false}""";

            assertThat(api.put(stranger, "/api/businesses/" + businessId + "/profile", profile))
                    .hasStatus(HttpStatus.FORBIDDEN);
            assertThat(api.post(stranger, "/api/businesses/" + businessId + "/branches", """
                            {"name":"Intrusa","street":"Calle 1","city":"CABA"}"""))
                    .hasStatus(HttpStatus.FORBIDDEN);

            assertThat(api.put(owner, "/api/businesses/" + businessId + "/profile", profile))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.category")
                    .isEqualTo("BEAUTY_SALON");
        }

        @Test
        void outsidersCannotSeeTheBusinessPrivateData() {
            var businessId = api.createBusiness(api.registerNewUser("Dueña"), slug());

            assertThat(api.get(api.registerNewUser("Ajena"), "/api/businesses/" + businessId))
                    .hasStatus(HttpStatus.FORBIDDEN);
        }

        @Test
        void changesAreRecordedInTheAuditLogThatOnlyTheOwnerReads() {
            var owner = api.registerNewUser("Dueña");
            var businessId = api.createBusiness(owner, slug());
            api.createBranch(owner, businessId, "Centro");

            var log = api.get(owner, "/api/businesses/" + businessId + "/audit-log");

            assertThat(log).hasStatusOk();
            assertThat(log).bodyJson().extractingPath("$.items[0].action").isEqualTo("branch.opened");
            assertThat(log).bodyJson().extractingPath("$.items[1].action").isEqualTo("business.registered");
            assertThat(log)
                    .bodyJson()
                    .extractingPath("$.items[0].actorUserId")
                    .isEqualTo(owner.userId().toString());
        }
    }

    @Test
    void invalidBranchDataIsRejected() {
        var owner = api.registerNewUser("Dueña");
        var businessId = api.createBusiness(owner, slug());

        assertThat(api.post(owner, "/api/businesses/" + businessId + "/branches", """
                        {"name":"Centro","street":"Calle 1","city":"CABA","timeZone":"Marte/Olympus"}"""))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("invalid_time_zone");
        assertThat(api.post(owner, "/api/businesses/" + businessId + "/branches", """
                        {"name":"Centro","street":"Calle 1","city":"CABA","latitude":-34.6}"""))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson()
                .extractingPath("$.code")
                .isEqualTo("invalid_coordinates");
    }

    static String slug() {
        return "negocio-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
