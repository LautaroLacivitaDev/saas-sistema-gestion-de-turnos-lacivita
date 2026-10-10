package com.lacivita.turnos.notifications.infrastructure;

import com.lacivita.turnos.notifications.domain.ChannelKind;
import com.lacivita.turnos.notifications.domain.DemoEmail;
import com.lacivita.turnos.notifications.domain.NotificationChannel;
import com.lacivita.turnos.notifications.domain.OutgoingMessage;
import com.lacivita.turnos.shared.domain.Email;
import java.nio.charset.StandardCharsets;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

/** Canal de email del modo demo: el aviso queda en el buzón de la demo, con su HTML y su {@code .ics}. */
@Component
@ConditionalOnBooleanProperty("app.demo.enabled")
class DemoEmailChannel implements NotificationChannel {

    private static final String CALENDAR = "text/calendar";

    private final DemoEmailStore store;

    DemoEmailChannel(DemoEmailStore store) {
        this.store = store;
    }

    @Override
    public ChannelKind kind() {
        return ChannelKind.EMAIL;
    }

    @Override
    public void send(OutgoingMessage message) {
        String calendar = message.attachments().stream()
                .filter(attachment -> attachment.contentType().startsWith(CALENDAR))
                .findFirst()
                .map(attachment -> new String(attachment.content(), StandardCharsets.UTF_8))
                .orElse(null);
        store.store(DemoEmail.rich(
                new Email(message.to()),
                message.subject(),
                message.text(),
                message.html(),
                calendar,
                store.clock().instant()));
    }
}
