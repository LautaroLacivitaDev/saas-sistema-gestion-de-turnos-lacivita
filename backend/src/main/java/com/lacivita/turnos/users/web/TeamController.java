package com.lacivita.turnos.users.web;

import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.web.PageResponse;
import com.lacivita.turnos.users.application.TeamInvitations;
import com.lacivita.turnos.users.application.TeamMembers;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Equipo de un negocio: miembros e invitaciones. */
@Tag(name = "Equipo")
@RestController
@RequestMapping("/api/businesses/{businessId}")
class TeamController {

    private static final int MAX_PAGE_SIZE = 100;

    private final TeamMembers members;
    private final TeamInvitations invitations;

    TeamController(TeamMembers members, TeamInvitations invitations) {
        this.members = members;
        this.invitations = invitations;
    }

    @Operation(summary = "Lista el equipo", description = "Dueño y gerentes.")
    @GetMapping("/members")
    PageResponse<TeamResponses.Member> members(
            @PathVariable UUID businessId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var pageable = PageRequest.of(Math.max(page, 0), clamp(size), Sort.by("createdAt"));
        return PageResponse.of(members.list(businessId, pageable), TeamResponses.Member::from);
    }

    @Operation(summary = "Cambia el rol de un miembro", description = "Solo el dueño: nombra o quita gerentes.")
    @PutMapping("/members/{userId}/role")
    TeamResponses.Member changeRole(
            @PathVariable UUID businessId,
            @PathVariable UUID userId,
            @Valid @RequestBody TeamRequests.ChangeRole body,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return TeamResponses.Member.from(members.changeRole(businessId, actor, userId, body.role()));
    }

    @Operation(summary = "Asigna las sucursales de un miembro")
    @PutMapping("/members/{userId}/branches")
    TeamResponses.Member assignBranches(
            @PathVariable UUID businessId,
            @PathVariable UUID userId,
            @Valid @RequestBody TeamRequests.AssignBranches body,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return TeamResponses.Member.from(members.assignBranches(businessId, actor, userId, body.branchIds()));
    }

    @Operation(summary = "Da de baja a un miembro")
    @DeleteMapping("/members/{userId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void remove(
            @PathVariable UUID businessId,
            @PathVariable UUID userId,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        members.remove(businessId, actor, userId);
    }

    @Operation(
            summary = "Invita a una persona al equipo",
            description = "El dueño invita gerentes y barberos; el gerente, barberos de sus sucursales.")
    @PostMapping("/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    TeamResponses.Invitation invite(
            @PathVariable UUID businessId,
            @Valid @RequestBody TeamRequests.Invite body,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        var view = invitations.invite(businessId, actor, new Email(body.email()), body.role(), body.branchIds());
        return TeamResponses.Invitation.from(view);
    }

    @Operation(summary = "Lista las invitaciones pendientes")
    @GetMapping("/invitations")
    PageResponse<TeamResponses.Invitation> pendingInvitations(
            @PathVariable UUID businessId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var pageable = PageRequest.of(Math.max(page, 0), clamp(size), Sort.by("createdAt"));
        return PageResponse.of(invitations.pending(businessId, pageable), TeamResponses.Invitation::from);
    }

    @Operation(summary = "Revoca una invitación pendiente")
    @DeleteMapping("/invitations/{invitationId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void revoke(
            @PathVariable UUID businessId,
            @PathVariable UUID invitationId,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        invitations.revoke(businessId, actor, invitationId);
    }

    private static int clamp(int size) {
        return Math.clamp(size, 1, MAX_PAGE_SIZE);
    }
}
