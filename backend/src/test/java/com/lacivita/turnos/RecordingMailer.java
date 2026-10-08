package com.lacivita.turnos;

import com.lacivita.turnos.shared.domain.Email;
import com.lacivita.turnos.shared.mail.MailMessage;
import com.lacivita.turnos.shared.mail.Mailer;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Pattern;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Reemplazo de {@link Mailer} para las pruebas: guarda los emails en memoria en lugar de enviarlos.
 * Respeta la misma regla que el real: solo registra si la transacción se confirma.
 */
public class RecordingMailer implements Mailer {

    private static final Pattern TOKEN = Pattern.compile("token=([A-Za-z0-9_%-]+)");

    private final List<MailMessage> sent = new CopyOnWriteArrayList<>();

    @Override
    public void sendAfterCommit(MailMessage message) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sent.add(message);
                }
            });
        } else {
            sent.add(message);
        }
    }

    public List<MailMessage> sentTo(String email) {
        var recipient = new Email(email);
        return sent.stream().filter(m -> m.to().equals(recipient)).toList();
    }

    /** Token del último email enviado a esa dirección. */
    public Optional<String> lastTokenSentTo(String email) {
        var messages = sentTo(email);
        if (messages.isEmpty()) {
            return Optional.empty();
        }
        var matcher = TOKEN.matcher(messages.getLast().body());
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }
}
