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

// DECISIÓN: los emails de la cuenta (y el código de la reserva como invitado) se envían después del
// commit y sin reintentos automáticos: los espera la persona en ese momento y, si no llegan, pide otro.
// No pasan por la bandeja de salida de notificaciones para no guardar tokens ni códigos en claro.
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
