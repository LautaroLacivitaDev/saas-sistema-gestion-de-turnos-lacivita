package com.lacivita.turnos.notifications.infrastructure;

import com.lacivita.turnos.notifications.domain.ChannelKind;
import com.lacivita.turnos.notifications.domain.DeliveryFailedException;
import com.lacivita.turnos.notifications.domain.NotificationChannel;
import com.lacivita.turnos.notifications.domain.OutgoingMessage;
import com.lacivita.turnos.shared.config.AppProperties;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

/**
 * Canal de email: HTML con su versión en texto plano y los adjuntos. En desarrollo sale a Mailpit; en
 * producción, al proveedor transaccional configurado en {@code spring.mail}.
 */
@Component
class SmtpEmailChannel implements NotificationChannel {

    private final JavaMailSender mailSender;
    private final AppProperties.Mail sender;

    SmtpEmailChannel(JavaMailSender mailSender, AppProperties properties) {
        this.mailSender = mailSender;
        this.sender = properties.mail();
    }

    @Override
    public ChannelKind kind() {
        return ChannelKind.EMAIL;
    }

    @Override
    public void send(OutgoingMessage message) {
        try {
            mailSender.send(mimeMessage -> {
                var helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
                helper.setFrom(sender.from(), sender.fromName());
                helper.setTo(message.to());
                helper.setSubject(message.subject());
                helper.setText(message.text(), message.html());
                for (var attachment : message.attachments()) {
                    helper.addAttachment(
                            attachment.fileName(),
                            new ByteArrayResource(attachment.content()),
                            attachment.contentType());
                }
            });
        } catch (MailException ex) {
            // No se registra el destinatario: es un dato personal.
            throw new DeliveryFailedException("smtp_error", ex);
        }
    }
}
