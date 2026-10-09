package com.lacivita.turnos.notifications.application;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Configuración de las notificaciones (prefijo {@code app.notifications}).
 *
 * @param sendOnCommit enviar apenas se confirma el cambio del turno, sin esperar a la tarea periódica. Las
 *     pruebas lo apagan para enviar cuando lo deciden.
 */
@ConfigurationProperties(prefix = "app.notifications")
public record NotificationProperties(@DefaultValue("true") boolean sendOnCommit) {}
