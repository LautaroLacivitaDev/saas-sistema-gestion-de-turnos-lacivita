package com.lacivita.turnos.schedule.application;

import com.lacivita.turnos.business.BusinessDirectory;
import com.lacivita.turnos.catalog.BookableItem;
import com.lacivita.turnos.schedule.FreeBarbers;
import com.lacivita.turnos.schedule.application.ScheduleViews.SlotBarberView;
import com.lacivita.turnos.schedule.application.ScheduleViews.SlotView;
import com.lacivita.turnos.schedule.domain.UnknownBranchException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Implementa la consulta de profesionales libres con el mismo cálculo que la disponibilidad pública: lo
 * que se ofrece es exactamente lo que se puede reservar.
 */
@Service
class FreeBarbersService implements FreeBarbers {

    private final BusinessDirectory businesses;
    private final AvailabilityReader availability;

    FreeBarbersService(BusinessDirectory businesses, AvailabilityReader availability) {
        this.businesses = businesses;
        this.availability = availability;
    }

    @Override
    public List<UUID> at(
            UUID businessId, UUID branchId, BookableItem item, UUID barberId, Instant start, UUID ignoringBooking) {
        var branch = businesses.branch(businessId, branchId).orElseThrow(UnknownBranchException::new);
        var date = LocalDate.ofInstant(start, branch.timeZone());
        return availability.read(businessId, branch, item, barberId, date, ignoringBooking).slots().stream()
                .filter(slot -> slot.startsAt().equals(start))
                .map(SlotView::barbers)
                .flatMap(List::stream)
                .map(SlotBarberView::barberId)
                .toList();
    }
}
