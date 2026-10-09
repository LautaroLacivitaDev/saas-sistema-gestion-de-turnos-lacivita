package com.lacivita.turnos.catalog.application;

import com.lacivita.turnos.business.BusinessDirectory;
import com.lacivita.turnos.catalog.application.CatalogViews.ProfessionalView;
import com.lacivita.turnos.catalog.domain.BarberNotFoundException;
import com.lacivita.turnos.catalog.domain.UnknownBusinessException;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Los profesionales en la página pública de un negocio, buscado por su link. */
@Service
public class PublicTeam {

    private final BusinessDirectory businesses;
    private final PublicTeamReader reader;

    PublicTeam(BusinessDirectory businesses, PublicTeamReader reader) {
        this.businesses = businesses;
        this.reader = reader;
    }

    public List<ProfessionalView> bySlug(String slug) {
        return reader.read(businessIdOf(slug));
    }

    public ProfessionalView one(String slug, UUID barberId) {
        return bySlug(slug).stream()
                .filter(professional -> professional.barberId().equals(barberId))
                .findFirst()
                .orElseThrow(BarberNotFoundException::new);
    }

    private UUID businessIdOf(String slug) {
        return businesses
                .findBySlug(slug)
                .orElseThrow(UnknownBusinessException::new)
                .id();
    }
}
