package com.lacivita.turnos.catalog.web;

import com.lacivita.turnos.catalog.application.BarberProfiles;
import com.lacivita.turnos.catalog.application.CatalogViews.ProfileView;
import com.lacivita.turnos.catalog.domain.ProfessionalProfile;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Catálogo: perfil de cada profesional")
@RestController
@RequestMapping("/api/businesses/{businessId}/barbers/{barberId}/profile")
class ProfileController {

    private final BarberProfiles profiles;

    ProfileController(BarberProfiles profiles) {
        this.profiles = profiles;
    }

    @Operation(summary = "Perfil público de un profesional", description = "Todo el equipo.")
    @GetMapping
    ProfileView profile(@PathVariable UUID businessId, @PathVariable UUID barberId) {
        return profiles.of(businessId, barberId);
    }

    @Operation(
            summary = "Cambia el perfil público",
            description = "El profesional, el gerente de sus sucursales o el dueño. Descripción hasta 500"
                    + " caracteres, hasta 6 especialidades y la foto como link https.")
    @PutMapping
    ProfileView change(
            @PathVariable UUID businessId,
            @PathVariable UUID barberId,
            @RequestBody ProfileData body,
            @AuthenticationPrincipal AuthenticatedUser actor) {
        return profiles.change(businessId, actor, barberId, body.profile());
    }

    record ProfileData(String bio, List<String> specialties, String photoUrl) {

        ProfessionalProfile profile() {
            return new ProfessionalProfile(bio, specialties, photoUrl);
        }
    }
}
