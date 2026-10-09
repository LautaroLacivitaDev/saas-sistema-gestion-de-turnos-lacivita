package com.lacivita.turnos.booking.application;

import com.lacivita.turnos.booking.application.BookingViews.AppointmentView;
import com.lacivita.turnos.booking.application.BookingViews.HoldView;
import com.lacivita.turnos.booking.domain.Customer;
import com.lacivita.turnos.booking.domain.UnknownBusinessException;
import com.lacivita.turnos.business.BusinessDirectory;
import com.lacivita.turnos.catalog.BookableItem;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;

/**
 * Reserva desde la página pública de un negocio (por su link). Averigua el negocio y delega en {@link
 * OnlineBooking}, que trabaja dentro de ese negocio.
 */
@Service
public class PublicBooking {

    private final BusinessDirectory businesses;
    private final OnlineBooking booking;

    PublicBooking(BusinessDirectory businesses, OnlineBooking booking) {
        this.businesses = businesses;
        this.booking = booking;
    }

    public HoldView hold(String slug, UUID branchId, BookableItem item, UUID barberId, Instant start) {
        return booking.hold(businessOf(slug), branchId, item, barberId, start);
    }

    public void sendGuestCode(String slug, UUID holdId, Customer.Contact contact, String humanToken, String remoteIp) {
        booking.sendGuestCode(businessOf(slug), holdId, contact, humanToken, remoteIp);
    }

    public AppointmentView confirm(String slug, UUID holdId, String code, AuthenticatedUser user) {
        return booking.confirm(businessOf(slug), holdId, code, user);
    }

    private UUID businessOf(String slug) {
        return businesses
                .findBySlug(slug)
                .orElseThrow(UnknownBusinessException::new)
                .id();
    }
}
