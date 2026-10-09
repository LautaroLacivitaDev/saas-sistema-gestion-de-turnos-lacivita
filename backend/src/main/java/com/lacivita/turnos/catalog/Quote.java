package com.lacivita.turnos.catalog;

import com.lacivita.turnos.shared.domain.Money;
import java.time.Duration;

/** Precio y duración con los que un profesional hace un servicio o combo hoy. */
public record Quote(Money price, Duration duration) {}
