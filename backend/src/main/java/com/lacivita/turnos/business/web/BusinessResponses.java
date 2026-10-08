package com.lacivita.turnos.business.web;

import com.lacivita.turnos.business.application.BusinessViews.BranchView;
import com.lacivita.turnos.business.application.BusinessViews.BusinessView;
import com.lacivita.turnos.business.application.BusinessViews.PublicBusinessPage;
import com.lacivita.turnos.business.application.BusinessViews.SlugCheck;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Respuestas HTTP del módulo de negocios: son el contrato de la API. */
final class BusinessResponses {

    private BusinessResponses() {}

    record Business(UUID id, String name, String slug, String category, String description, boolean searchable) {

        static Business from(BusinessView view) {
            return new Business(
                    view.id(), view.name(), view.slug(), view.category(), view.description(), view.searchable());
        }
    }

    record Branch(
            UUID id,
            String name,
            String street,
            String neighborhood,
            String city,
            BigDecimal latitude,
            BigDecimal longitude,
            String phone,
            String timeZone) {

        static Branch from(BranchView view) {
            return new Branch(
                    view.id(),
                    view.name(),
                    view.street(),
                    view.neighborhood(),
                    view.city(),
                    view.latitude(),
                    view.longitude(),
                    view.phone(),
                    view.timeZone());
        }
    }

    record PublicPage(String canonicalSlug, String name, String category, String description, List<Branch> branches) {

        static PublicPage from(PublicBusinessPage page) {
            return new PublicPage(
                    page.canonicalSlug(),
                    page.name(),
                    page.category(),
                    page.description(),
                    page.branches().stream().map(Branch::from).toList());
        }
    }

    record SlugAvailability(String slug, boolean available, String code, String message) {

        static SlugAvailability from(SlugCheck check) {
            return new SlugAvailability(check.slug(), check.available(), check.code(), check.message());
        }
    }
}
