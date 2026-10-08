package com.lacivita.turnos.business.application;

import com.lacivita.turnos.business.application.BusinessViews.PublicBusinessPage;
import com.lacivita.turnos.business.domain.BranchRepository;
import com.lacivita.turnos.business.domain.BusinessNotFoundException;
import com.lacivita.turnos.business.domain.SlugClaimRepository;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Página pública de reservas de un negocio. No exige sesión. Se llega también con un slug viejo: la
 * respuesta indica el slug actual para que el frontend redirija.
 */
// DECISIÓN: la redirección la hace el frontend con el slug canónico de la respuesta, en lugar de un 301
// de la API. Next.js sigue los redirects de fetch en silencio y el navegador no vería la URL nueva.
@Service
public class PublicBusinessPages {

    private final SlugClaimRepository slugClaims;
    private final BranchRepository branches;
    private final BusinessMapper mapper;

    PublicBusinessPages(SlugClaimRepository slugClaims, BranchRepository branches, BusinessMapper mapper) {
        this.slugClaims = slugClaims;
        this.branches = branches;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public PublicBusinessPage bySlug(String slug) {
        var business = slugClaims
                .findBusinessBySlug(slug.strip().toLowerCase(Locale.ROOT))
                .orElseThrow(BusinessNotFoundException::new);
        var profile = business.profile();
        var branchViews = branches.findAllByBusinessIdOrderByCreatedAtAsc(business.getId()).stream()
                .map(mapper::toView)
                .toList();
        return new PublicBusinessPage(
                business.getSlug(), profile.name(), profile.category().name(), profile.description(), branchViews);
    }
}
