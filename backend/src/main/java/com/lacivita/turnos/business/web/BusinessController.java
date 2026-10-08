package com.lacivita.turnos.business.web;

import com.lacivita.turnos.business.application.BusinessProfiles;
import com.lacivita.turnos.business.application.BusinessRegistration;
import com.lacivita.turnos.business.application.SlugAvailability;
import com.lacivita.turnos.business.domain.Slug;
import com.lacivita.turnos.shared.domain.Ids;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Negocios")
@RestController
@RequestMapping("/api/businesses")
class BusinessController {

    private final BusinessRegistration registration;
    private final BusinessProfiles profiles;
    private final SlugAvailability slugAvailability;

    BusinessController(
            BusinessRegistration registration, BusinessProfiles profiles, SlugAvailability slugAvailability) {
        this.registration = registration;
        this.profiles = profiles;
        this.slugAvailability = slugAvailability;
    }

    @Operation(summary = "Crea un negocio", description = "Quien lo crea queda como dueño.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    BusinessResponses.Business register(
            @Valid @RequestBody BusinessRequests.Register body, @AuthenticationPrincipal AuthenticatedUser user) {
        var view = registration.register(Ids.newId(), user, body.profile(), new Slug(body.slug()));
        return BusinessResponses.Business.from(view);
    }

    @Operation(summary = "Verifica si un link está disponible")
    @GetMapping("/slug-availability")
    BusinessResponses.SlugAvailability slugAvailability(@RequestParam String slug) {
        return BusinessResponses.SlugAvailability.from(slugAvailability.check(slug));
    }

    @Operation(summary = "Datos del negocio para su equipo")
    @GetMapping("/{businessId}")
    BusinessResponses.Business view(@PathVariable UUID businessId) {
        return BusinessResponses.Business.from(profiles.view(businessId));
    }

    @Operation(summary = "Cambia los datos del negocio", description = "Solo el dueño.")
    @PutMapping("/{businessId}/profile")
    BusinessResponses.Business updateProfile(
            @PathVariable UUID businessId, @Valid @RequestBody BusinessRequests.UpdateProfile body) {
        return BusinessResponses.Business.from(profiles.updateProfile(businessId, body.profile()));
    }

    @Operation(
            summary = "Cambia el link del negocio",
            description = "Solo el dueño. El link anterior sigue funcionando y redirige al nuevo.")
    @PutMapping("/{businessId}/slug")
    BusinessResponses.Business changeSlug(
            @PathVariable UUID businessId, @Valid @RequestBody BusinessRequests.ChangeSlug body) {
        return BusinessResponses.Business.from(profiles.changeSlug(businessId, new Slug(body.slug())));
    }
}
