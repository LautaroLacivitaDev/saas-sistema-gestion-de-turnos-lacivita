package com.lacivita.turnos.catalog.application;

import com.lacivita.turnos.business.BusinessDirectory;
import com.lacivita.turnos.catalog.application.CatalogViews.PublicCatalogView;
import com.lacivita.turnos.catalog.domain.UnknownBusinessException;
import org.springframework.stereotype.Component;

/** Catálogo de la página pública de un negocio. No exige sesión. Acepta también links viejos. */
@Component
public class PublicCatalog {

    private final BusinessDirectory businesses;
    private final PublicCatalogReader reader;

    PublicCatalog(BusinessDirectory businesses, PublicCatalogReader reader) {
        this.businesses = businesses;
        this.reader = reader;
    }

    public PublicCatalogView bySlug(String slug) {
        var business = businesses.findBySlug(slug).orElseThrow(UnknownBusinessException::new);
        return reader.read(business.id());
    }
}
