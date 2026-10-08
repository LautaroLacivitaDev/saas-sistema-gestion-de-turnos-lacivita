package com.lacivita.turnos.users.web;

import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.users.application.InvitationAcceptance;
import com.lacivita.turnos.users.application.MyMemberships;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Negocios de la persona con sesión iniciada y aceptación de invitaciones. */
@Tag(name = "Equipo")
@RestController
class MembershipController {

    private final MyMemberships myMemberships;
    private final InvitationAcceptance acceptance;

    MembershipController(MyMemberships myMemberships, InvitationAcceptance acceptance) {
        this.myMemberships = myMemberships;
        this.acceptance = acceptance;
    }

    @Operation(summary = "Negocios en los que trabaja la persona con sesión iniciada")
    @GetMapping("/api/memberships")
    List<TeamResponses.Membership> mine(@AuthenticationPrincipal AuthenticatedUser user) {
        return myMemberships.of(user.id()).stream()
                .map(TeamResponses.Membership::from)
                .toList();
    }

    @Operation(
            summary = "Acepta una invitación",
            description = "La cuenta con sesión iniciada tiene que tener el mismo email que la invitación.")
    @PostMapping("/api/invitations/accept")
    TeamResponses.Membership accept(
            @Valid @RequestBody TeamRequests.AcceptInvitation body, @AuthenticationPrincipal AuthenticatedUser user) {
        return TeamResponses.Membership.from(acceptance.accept(body.token(), user));
    }
}
