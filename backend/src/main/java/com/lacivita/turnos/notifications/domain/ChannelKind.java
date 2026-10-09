package com.lacivita.turnos.notifications.domain;

/** Por dónde sale una notificación. En la Fase 1, solo email; cada canal nuevo implementa {@link NotificationChannel}. */
public enum ChannelKind {
    EMAIL
}
