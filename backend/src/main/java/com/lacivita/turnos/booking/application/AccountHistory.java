package com.lacivita.turnos.booking.application;

import com.lacivita.turnos.booking.application.BookingViews.AccountAppointmentView;
import com.lacivita.turnos.booking.domain.AppointmentRepository;
import com.lacivita.turnos.booking.domain.AppointmentRepository.AccountAppointment;
import com.lacivita.turnos.booking.domain.AppointmentStatus;
import com.lacivita.turnos.business.BusinessDirectory;
import com.lacivita.turnos.business.BusinessSummary;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.tenancy.TenantContext;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

/**
 * Historial de turnos de una persona como cliente, en todos los negocios donde reservó con su cuenta. Sirve
 * para ver sus próximos turnos y para "repetir la última reserva".
 *
 * <p>Averiguar en qué negocios tiene turnos cruza negocios (operación de sistema, filtrada por la cuenta);
 * los datos de cada turno se leen después dentro de su negocio.
 */
// DECISIÓN: el historial muestra los últimos 50 turnos. Alcanza para "mis turnos" en el celular; si hace
// falta más, se pagina.
@Service
public class AccountHistory {

    static final int MAX_APPOINTMENTS = 50;

    private final AppointmentRepository appointments;
    private final AccountHistoryReader reader;
    private final BusinessDirectory businesses;

    AccountHistory(AppointmentRepository appointments, AccountHistoryReader reader, BusinessDirectory businesses) {
        this.appointments = appointments;
        this.reader = reader;
        this.businesses = businesses;
    }

    /** Del más nuevo al más viejo. */
    public List<AccountAppointmentView> of(AuthenticatedUser user) {
        List<AccountAppointment> found = TenantContext.callAsSystem(
                "historial del cliente: sus turnos en todos los negocios",
                () -> appointments.findForAccount(user.id(), AppointmentStatus.IN_AGENDA, Limit.of(MAX_APPOINTMENTS)));
        Map<UUID, List<UUID>> idsByBusiness = found.stream()
                .collect(Collectors.groupingBy(
                        AccountAppointment::getBusinessId,
                        LinkedHashMap::new,
                        Collectors.mapping(AccountAppointment::getAppointmentId, Collectors.toList())));
        if (idsByBusiness.isEmpty()) {
            return List.of();
        }
        Map<UUID, BusinessSummary> summaries = businesses.findAll(idsByBusiness.keySet()).stream()
                .collect(Collectors.toMap(BusinessSummary::id, Function.identity()));
        return idsByBusiness.entrySet().stream()
                .filter(entry -> summaries.containsKey(entry.getKey()))
                .flatMap(entry -> {
                    var business = summaries.get(entry.getKey());
                    return reader.views(entry.getKey(), entry.getValue()).stream()
                            .map(view -> new AccountAppointmentView(business.name(), business.slug(), view));
                })
                .sorted(Comparator.comparing((AccountAppointmentView item) ->
                                item.appointment().startsAt())
                        .reversed())
                .toList();
    }
}
