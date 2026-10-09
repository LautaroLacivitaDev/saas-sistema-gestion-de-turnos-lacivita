package com.lacivita.turnos.notifications.application;

import com.lacivita.turnos.notifications.application.NotificationViews.NoticeView;
import com.lacivita.turnos.notifications.domain.AppNoticeRepository;
import com.lacivita.turnos.notifications.domain.NoticeNotFoundException;
import com.lacivita.turnos.shared.security.AuthenticatedUser;
import com.lacivita.turnos.shared.tenancy.BusinessId;
import com.lacivita.turnos.shared.tenancy.BusinessScoped;
import java.time.Clock;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Avisos en la app de cada persona del equipo. Cada uno ve y marca solo los suyos. */
@Service
public class Notices {

    private final AppNoticeRepository notices;
    private final Clock clock;

    Notices(AppNoticeRepository notices, Clock clock) {
        this.notices = notices;
        this.clock = clock;
    }

    @BusinessScoped
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#businessId, 'Business', 'BARBER')")
    public Page<NoticeView> mine(@BusinessId UUID businessId, AuthenticatedUser actor, Pageable pageable) {
        return notices.findByUserIdOrderByCreatedAtDesc(actor.id(), pageable).map(NoticeView::of);
    }

    @BusinessScoped
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#businessId, 'Business', 'BARBER')")
    public long unreadCount(@BusinessId UUID businessId, AuthenticatedUser actor) {
        return notices.countByUserIdAndReadAtIsNull(actor.id());
    }

    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'BARBER')")
    public NoticeView markRead(@BusinessId UUID businessId, AuthenticatedUser actor, UUID noticeId) {
        var notice = notices.findById(noticeId)
                .filter(found -> found.belongsTo(actor.id()))
                .orElseThrow(NoticeNotFoundException::new);
        notice.markRead(clock.instant());
        return NoticeView.of(notice);
    }

    @BusinessScoped
    @Transactional
    @PreAuthorize("hasPermission(#businessId, 'Business', 'BARBER')")
    public void markAllRead(@BusinessId UUID businessId, AuthenticatedUser actor) {
        notices.markAllRead(actor.id(), clock.instant());
    }
}
