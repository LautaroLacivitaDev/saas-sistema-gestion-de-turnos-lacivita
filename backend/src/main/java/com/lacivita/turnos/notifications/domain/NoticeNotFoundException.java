package com.lacivita.turnos.notifications.domain;

import com.lacivita.turnos.shared.domain.NotFoundException;

public class NoticeNotFoundException extends NotFoundException {

    public NoticeNotFoundException() {
        super("notice_not_found", "No encontramos ese aviso.");
    }
}
