package com.lacivita.turnos;

import com.lacivita.turnos.notifications.domain.ChannelKind;
import com.lacivita.turnos.notifications.domain.DeliveryFailedException;
import com.lacivita.turnos.notifications.domain.NotificationChannel;
import com.lacivita.turnos.notifications.domain.OutgoingMessage;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;

/**
 * Reemplazo del canal de email de las notificaciones para las pruebas: guarda los mensajes en memoria. Se
 * le puede pedir que falle para un destinatario, para probar los reintentos sin afectar otras pruebas.
 */
public class RecordingNotificationChannel implements NotificationChannel {

    private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_%-]+)");

    private final List<OutgoingMessage> sent = new CopyOnWriteArrayList<>();
    private final Map<String, Integer> failuresLeft = new HashMap<>();

    @Override
    public ChannelKind kind() {
        return ChannelKind.EMAIL;
    }

    @Override
    public void send(OutgoingMessage message) {
        if (consumeFailure(message.to())) {
            throw new DeliveryFailedException("smtp_error", new IllegalStateException("Falla simulada"));
        }
        sent.add(message);
    }

    /** Los próximos envíos a esa dirección fallan, como si el proveedor de email no respondiera. */
    public synchronized void failNextTo(String email, int times) {
        failuresLeft.put(email, times);
    }

    public List<OutgoingMessage> sentTo(String email) {
        return sent.stream().filter(message -> message.to().equals(email)).toList();
    }

    /** Token del link para gestionar el turno del último email enviado a esa dirección. */
    public Optional<String> lastManageTokenSentTo(String email) {
        var messages = sentTo(email);
        if (messages.isEmpty()) {
            return Optional.empty();
        }
        var matcher = TOKEN.matcher(messages.getLast().text());
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    private synchronized boolean consumeFailure(String to) {
        int left = failuresLeft.getOrDefault(to, 0);
        if (left == 0) {
            return false;
        }
        failuresLeft.put(to, left - 1);
        return true;
    }
}
