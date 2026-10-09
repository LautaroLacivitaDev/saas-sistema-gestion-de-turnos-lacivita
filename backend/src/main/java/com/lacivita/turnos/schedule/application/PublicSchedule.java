package com.lacivita.turnos.schedule.application;

import com.lacivita.turnos.business.BusinessDirectory;
import com.lacivita.turnos.business.BusinessSummary;
import com.lacivita.turnos.catalog.BookableItem;
import com.lacivita.turnos.schedule.application.ScheduleViews.AvailabilityView;
import com.lacivita.turnos.schedule.application.ScheduleViews.WeeklyHoursView;
import com.lacivita.turnos.schedule.domain.UnknownBranchException;
import com.lacivita.turnos.schedule.domain.UnknownBusinessException;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** Agenda de la página pública: horarios de atención y horarios libres. No exige sesión. */
@Service
public class PublicSchedule {

    private final BusinessDirectory businesses;
    private final AvailabilityReader availability;
    private final BranchHours branchHours;

    PublicSchedule(BusinessDirectory businesses, AvailabilityReader availability, BranchHours branchHours) {
        this.businesses = businesses;
        this.availability = availability;
        this.branchHours = branchHours;
    }

    public WeeklyHoursView branchHours(String slug, UUID branchId) {
        return branchHours.of(business(slug).id(), branchId);
    }

    /** @param barberId un profesional en particular; {@code null} para "cualquiera disponible" */
    public AvailabilityView availability(String slug, UUID branchId, BookableItem item, UUID barberId, LocalDate date) {
        var business = business(slug);
        var branch = businesses.branch(business.id(), branchId).orElseThrow(UnknownBranchException::new);
        return availability.read(business.id(), branch, item, barberId, date, null);
    }

    private BusinessSummary business(String slug) {
        return businesses.findBySlug(slug).orElseThrow(UnknownBusinessException::new);
    }
}
