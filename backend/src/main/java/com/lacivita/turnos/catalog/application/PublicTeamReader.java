package com.lacivita.turnos.catalog.application;

import com.lacivita.turnos.catalog.application.CatalogViews.ProfessionalServiceView;
import com.lacivita.turnos.catalog.application.CatalogViews.ProfessionalView;
import com.lacivita.turnos.catalog.domain.BarberProfile;
import com.lacivita.turnos.catalog.domain.BarberProfileRepository;
import com.lacivita.turnos.catalog.domain.ProfessionalProfile;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import com.lacivita.turnos.users.TeamDirectory;
import com.lacivita.turnos.users.TeamMember;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lee los profesionales de la página pública dentro del negocio: los que hacen al menos un servicio, con su
 * perfil, sus sucursales y sus precios.
 */
@Component
class PublicTeamReader {

    private final PublicCatalogReader catalog;
    private final BarberProfileRepository profiles;
    private final TeamDirectory team;

    PublicTeamReader(PublicCatalogReader catalog, BarberProfileRepository profiles, TeamDirectory team) {
        this.catalog = catalog;
        this.profiles = profiles;
        this.team = team;
    }

    /** Ordenados por nombre. */
    @BusinessScoped
    @Transactional(readOnly = true)
    public List<ProfessionalView> read(@BusinessId UUID businessId) {
        Map<UUID, List<ProfessionalServiceView>> servicesByBarber = new LinkedHashMap<>();
        for (var service : catalog.read(businessId).services()) {
            for (var terms : service.barbers()) {
                servicesByBarber
                        .computeIfAbsent(terms.barberId(), id -> new ArrayList<>())
                        .add(new ProfessionalServiceView(
                                service.id(), service.name(), terms.price(), terms.durationMinutes()));
            }
        }
        if (servicesByBarber.isEmpty()) {
            return List.of();
        }
        Map<UUID, ProfessionalProfile> profilesByBarber =
                profiles.findAllByBarberIdIn(servicesByBarber.keySet()).stream()
                        .collect(Collectors.toMap(BarberProfile::getBarberId, BarberProfile::profile));
        Map<UUID, TeamMember> members = team.members(businessId, servicesByBarber.keySet()).stream()
                .collect(Collectors.toMap(TeamMember::userId, Function.identity()));
        return servicesByBarber.entrySet().stream()
                .filter(entry -> members.containsKey(entry.getKey()))
                .map(entry -> view(
                        members.get(entry.getKey()),
                        profilesByBarber.getOrDefault(entry.getKey(), ProfessionalProfile.EMPTY),
                        entry.getValue()))
                .sorted(Comparator.comparing(ProfessionalView::name))
                .toList();
    }

    private static ProfessionalView view(
            TeamMember member, ProfessionalProfile profile, List<ProfessionalServiceView> services) {
        return new ProfessionalView(
                member.userId(),
                member.name(),
                profile.bio(),
                profile.specialties(),
                profile.photoUrl(),
                member.membership().branchIds().stream().sorted().toList(),
                services.stream()
                        .sorted(Comparator.comparing(ProfessionalServiceView::price)
                                .thenComparing(ProfessionalServiceView::name))
                        .toList());
    }
}
