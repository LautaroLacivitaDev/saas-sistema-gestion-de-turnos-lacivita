package com.lacivita.turnos.shared.mail;

import com.lacivita.turnos.shared.config.AppProperties;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

// DECISIÓN: los emails de la cuenta se envían después del commit y sin reintentos automáticos.
// Si el envío falla, la persona puede pedir el link de nuevo. El outbox con reintentos llega con
// el módulo de notificaciones (Hito 7). No se usan eventos persistidos de Modulith para no guardar
// el token en claro en la tabla event_publication.
@Component
class SmtpMailer implements Mailer {

    private static final Logger log = LoggerFactory.getLogger(SmtpMailer.class);

    private final JavaMailSender mailSender;
    private final TaskExecutor executor;
    private final AppProperties.Mail sender;

    SmtpMailer(
            JavaMailSender mailSender,
            @Qualifier("applicationTaskExecutor") TaskExecutor executor,
            AppProperties properties) {
        this.mailSender = mailSender;
        this.executor = executor;
        this.sender = properties.mail();
    }

    @Override
    public void sendAfterCommit(MailMessage message) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    executor.execute(() -> send(message));
                }
            });
        } else {
            executor.execute(() -> send(message));
        }
    }

    private void send(MailMessage message) {
        try {
            mailSender.send(mimeMessage -> {
                var helper = new MimeMessageHelper(mimeMessage, StandardCharsets.UTF_8.name());
                helper.setFrom(sender.from(), sender.fromName());
                helper.setTo(message.to().value());
                helper.setSubject(message.subject());
                helper.setText(message.body());
            });
        } catch (MailException ex) {
            // No se loguea el destinatario: es un dato personal.
            log.error("No se pudo enviar el email '{}'", message.subject(), ex);
        }
    }
}
