package com.lacivita.turnos.users.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.lacivita.turnos.ApiClient;
import com.lacivita.turnos.ApiClient.Session;
import com.lacivita.turnos.IntegrationTest;
import com.lacivita.turnos.RecordingMailer;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;

@IntegrationTest
class TeamApiIntegrationTests {

    @Autowired
    MockMvcTester mvc;

    @Autowired
    RecordingMailer mailer;

    ApiClient api;
    Session owner;
    UUID businessId;
    UUID centro;
    UUID norte;

    @BeforeEach
    void setUp() {
        api = new ApiClient(mvc);
        owner = api.registerNewUser("Dueña");
        businessId = api.createBusiness(
                owner, "equipo-" + UUID.randomUUID().toString().substring(0, 8));
        centro = api.createBranch(owner, businessId, "Centro");
        norte = api.createBranch(owner, businessId, "Norte");
    }

    @Nested
    class Invitations {

        @Test
        void theInviteeJoinsWithTheRoleAndBranchesOfTheInvitation() {
            var barber = api.registerNewUser("Barbero");

            assertThat(invite(owner, barber.email(), "BARBER", centro)).hasStatus(HttpStatus.CREATED);
            var joined = accept(barber);

            assertThat(joined).hasStatusOk();
            assertThat(joined).bodyJson().extractingPath("$.role").isEqualTo("BARBER");
            assertThat(api.get(owner, members()))
                    .bodyJson()
                    .extractingPath("$.totalItems")
                    .isEqualTo(2);
        }

        @Test
        void onlyTheInvitedEmailCanAccept() {
            var invitee = api.registerNewUser("Invitada");
            var intruder = api.registerNewUser("Intrusa");
            invite(owner, invitee.email(), "BARBER", centro);
            String token = mailer.lastTokenSentTo(invitee.email()).orElseThrow();

            var result = api.post(intruder, "/api/invitations/accept", """
                    {"token":"%s"}""".formatted(token));

            assertThat(result).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
            assertThat(result).bodyJson().extractingPath("$.code").isEqualTo("invitation_email_mismatch");
        }

        @Test
        void anInvitationWorksOnlyOnce() {
            var barber = api.registerNewUser("Barbero");
            invite(owner, barber.email(), "BARBER", centro);
            accept(barber);

            assertThat(accept(barber)).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        }

        @Test
        void aRevokedInvitationCannotBeAccepted() {
            var barber = api.registerNewUser("Barbero");
            var invitationId = ApiClient.read(invite(owner, barber.email(), "BARBER", centro), "$.id");

            assertThat(api.delete(owner, "/api/businesses/" + businessId + "/invitations/" + invitationId))
                    .hasStatus(HttpStatus.NO_CONTENT);

            assertThat(accept(barber)).hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
        }

        @Test
        void reInvitingReplacesThePreviousLink() {
            var barber = api.registerNewUser("Barbero");
            invite(owner, barber.email(), "BARBER", centro);
            String oldToken = mailer.lastTokenSentTo(barber.email()).orElseThrow();
            invite(owner, barber.email(), "BARBER", norte);

            assertThat(api.post(barber, "/api/invitations/accept", """
                            {"token":"%s"}""".formatted(oldToken)))
                    .hasStatus(HttpStatus.UNPROCESSABLE_CONTENT);
            assertThat(accept(barber)).hasStatusOk();
        }

        @Test
        void existingMembersCannotBeInvitedAgain() {
            assertThat(invite(owner, owner.email(), "BARBER", centro)).hasStatus(HttpStatus.CONFLICT);
        }

        @Test
        void aPersonSeesTheirMembershipsInEveryBusiness() {
            // Lo resuelve Row Level Security con la persona de la sesión, sin negocio en el contexto.
            var barber = join("Barbero", "BARBER", centro);
            var otherOwner = api.registerNewUser("Otra dueña");
            var otherBusiness = api.createBusiness(
                    otherOwner, "otro-" + UUID.randomUUID().toString().substring(0, 8));
            var otherBranch = api.createBranch(otherOwner, otherBusiness, "Sur");
            api.post(otherOwner, "/api/businesses/" + otherBusiness + "/invitations", """
                    {"email":"%s","role":"MANAGER","branchIds":["%s"]}""".formatted(
                            barber.email(), otherBranch));
            assertThat(accept(barber)).hasStatusOk();

            assertThat(api.get(barber, "/api/memberships"))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$[*].role")
                    .asArray()
                    .containsExactly("BARBER", "MANAGER");
        }

        @Test
        void branchesMustBelongToTheBusiness() {
            assertThat(invite(owner, "alguien@example.com", "BARBER", UUID.randomUUID()))
                    .hasStatus(HttpStatus.BAD_REQUEST)
                    .bodyJson()
                    .extractingPath("$.code")
                    .isEqualTo("invalid_branches");
        }
    }

    @Nested
    class Rules {

        @Test
        void aManagerInvitesBarbersOfTheirBranchesButNotManagers() {
            var manager = join("Gerente", "MANAGER", centro);

            assertThat(invite(manager, "barbero-" + UUID.randomUUID() + "@example.com", "BARBER", centro))
                    .hasStatus(HttpStatus.CREATED);
            assertThat(invite(manager, "barbero-" + UUID.randomUUID() + "@example.com", "BARBER", norte))
                    .hasStatus(HttpStatus.FORBIDDEN)
                    .bodyJson()
                    .extractingPath("$.code")
                    .isEqualTo("outside_your_branches");
            assertThat(invite(manager, "gerente-" + UUID.randomUUID() + "@example.com", "MANAGER", centro))
                    .hasStatus(HttpStatus.FORBIDDEN)
                    .bodyJson()
                    .extractingPath("$.code")
                    .isEqualTo("only_owner_manages_managers");
        }

        @Test
        void barbersCannotSeeOrManageTheTeam() {
            var barber = join("Barbero", "BARBER", centro);

            assertThat(api.get(barber, members())).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(invite(barber, "otro@example.com", "BARBER", centro)).hasStatus(HttpStatus.FORBIDDEN);
        }

        @Test
        void onlyTheOwnerPromotesABarberToManager() {
            var manager = join("Gerente", "MANAGER", centro);
            var barber = join("Barbero", "BARBER", centro);
            String body = """
                    {"role":"MANAGER"}""";

            assertThat(api.put(manager, member(barber) + "/role", body)).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(api.put(owner, member(barber) + "/role", body))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.role")
                    .isEqualTo("MANAGER");
        }

        @Test
        void aManagerRemovesBarbersButNotOtherManagersNorTheOwner() {
            var manager = join("Gerente", "MANAGER", centro);
            var otherManager = join("Otro gerente", "MANAGER", centro);
            var barber = join("Barbero", "BARBER", centro);

            assertThat(api.delete(manager, member(otherManager))).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(api.delete(manager, member(owner))).hasStatus(HttpStatus.FORBIDDEN);
            assertThat(api.delete(manager, member(barber))).hasStatus(HttpStatus.NO_CONTENT);

            assertThat(api.get(barber, "/api/businesses/" + businessId)).hasStatus(HttpStatus.FORBIDDEN);
        }

        @Test
        void theOwnerCannotBeRemoved() {
            assertThat(api.delete(owner, member(owner)))
                    .hasStatus(HttpStatus.FORBIDDEN)
                    .bodyJson()
                    .extractingPath("$.code")
                    .isEqualTo("owner_is_fixed");
        }

        @Test
        void theOwnerReassignsBranches() {
            var barber = join("Barbero", "BARBER", centro);

            assertThat(api.put(owner, member(barber) + "/branches", """
                            {"branchIds":["%s"]}""".formatted(norte)))
                    .hasStatusOk()
                    .bodyJson()
                    .extractingPath("$.branchIds[0]")
                    .isEqualTo(norte.toString());
        }
    }

    private Session join(String name, String role, UUID branch) {
        var person = api.registerNewUser(name);
        invite(owner, person.email(), role, branch);
        assertThat(accept(person)).hasStatusOk();
        return person;
    }

    private MvcTestResult invite(Session actor, String email, String role, UUID branch) {
        return api.post(actor, "/api/businesses/" + businessId + "/invitations", """
                {"email":"%s","role":"%s","branchIds":["%s"]}""".formatted(email, role, branch));
    }

    private MvcTestResult accept(Session invitee) {
        String token = mailer.lastTokenSentTo(invitee.email()).orElseThrow();
        return api.post(invitee, "/api/invitations/accept", """
                {"token":"%s"}""".formatted(token));
    }

    private String members() {
        return "/api/businesses/" + businessId + "/members";
    }

    private String member(Session person) {
        return members() + "/" + person.userId();
    }
}
