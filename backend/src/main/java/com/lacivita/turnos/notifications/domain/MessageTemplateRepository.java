package com.lacivita.turnos.notifications.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.Repository;

public interface MessageTemplateRepository extends Repository<MessageTemplate, UUID> {

    MessageTemplate save(MessageTemplate template);

    void delete(MessageTemplate template);

    List<MessageTemplate> findAll();

    Optional<MessageTemplate> findByType(NotificationType type);

    /** El texto que rige para el negocio: el propio o, si no tiene, el de Laciturnos. */
    default TemplateText textFor(NotificationType type) {
        return findByType(type).map(MessageTemplate::text).orElseGet(() -> MessageTemplate.defaultFor(type));
    }
}
