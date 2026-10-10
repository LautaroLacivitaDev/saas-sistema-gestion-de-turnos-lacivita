// Formatos en español rioplatense. Las fechas se muestran siempre en la zona horaria de la sucursal, no en
// la del celular de quien mira (alguien de viaje ve la hora real del turno).

const LOCALE = "es-AR";

/** Por ejemplo "$ 9.000" o "$ 9.500,50". */
export function money(amount: number): string {
  const whole = Number.isInteger(amount);
  return new Intl.NumberFormat(LOCALE, {
    style: "currency",
    currency: "ARS",
    minimumFractionDigits: whole ? 0 : 2,
    maximumFractionDigits: whole ? 0 : 2,
  }).format(amount);
}

/** Por ejemplo "viernes 9 de octubre a las 10:00", como en los emails. */
export function dateTime(instant: string, timeZone: string): string {
  const parts = new Intl.DateTimeFormat(LOCALE, {
    weekday: "long",
    day: "numeric",
    month: "long",
    timeZone,
  }).formatToParts(new Date(instant));
  const part = (type: string) =>
    parts.find((p) => p.type === type)?.value ?? "";
  return `${part("weekday")} ${part("day")} de ${part("month")} a las ${time(instant, timeZone)}`;
}

/** Por ejemplo "10:00". */
export function time(instant: string, timeZone: string): string {
  return new Intl.DateTimeFormat(LOCALE, {
    hour: "2-digit",
    minute: "2-digit",
    hourCycle: "h23",
    timeZone,
  }).format(new Date(instant));
}

/** Fecha local (AAAA-MM-DD) de un instante en una zona horaria. */
export function localDate(instant: Date, timeZone: string): string {
  const parts = new Intl.DateTimeFormat("en-CA", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    timeZone,
  }).formatToParts(instant);
  const part = (type: string) =>
    parts.find((p) => p.type === type)?.value ?? "";
  return `${part("year")}-${part("month")}-${part("day")}`;
}

/** Diferencia (en minutos) entre la hora de la zona y UTC en ese instante. */
function offsetMinutes(instant: Date, timeZone: string): number {
  const parts = new Intl.DateTimeFormat("en-US", {
    timeZone,
    hourCycle: "h23",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  }).formatToParts(instant);
  const part = (type: string) =>
    Number(parts.find((p) => p.type === type)?.value);
  const asUtc = Date.UTC(
    part("year"),
    part("month") - 1,
    part("day"),
    part("hour"),
    part("minute"),
  );
  return Math.round(
    (asUtc - Math.floor(instant.getTime() / 60_000) * 60_000) / 60_000,
  );
}

/**
 * El instante (ISO, en UTC) de una fecha y hora de la sucursal: "2026-10-09" a las "10:00" en Buenos Aires
 * es "2026-10-09T13:00:00.000Z". Sirve para mandar al backend lo que alguien eligió en la agenda.
 */
export function zonedInstant(
  date: string,
  hhmm: string,
  timeZone: string,
): string {
  const [year, month, day] = date.split("-").map(Number);
  const [hour, minute] = hhmm.split(":").map(Number);
  const wanted = Date.UTC(year, month - 1, day, hour, minute);
  // Dos pasadas: el desfase de la zona puede cambiar justo en ese día (horario de verano).
  let guess = wanted - offsetMinutes(new Date(wanted), timeZone) * 60_000;
  guess = wanted - offsetMinutes(new Date(guess), timeZone) * 60_000;
  return new Date(guess).toISOString();
}

/** Suma días a una fecha AAAA-MM-DD, sin zonas horarias de por medio. */
export function addDays(date: string, days: number): string {
  const [year, month, day] = date.split("-").map(Number);
  const result = new Date(Date.UTC(year, month - 1, day + days));
  return result.toISOString().slice(0, 10);
}

/** Por ejemplo { weekday: "vie", day: "10", month: "oct" } para los botones de días. */
export function dayParts(date: string): {
  weekday: string;
  day: string;
  month: string;
} {
  const [year, month, day] = date.split("-").map(Number);
  const value = new Date(Date.UTC(year, month - 1, day, 12));
  const format = (options: Intl.DateTimeFormatOptions) =>
    new Intl.DateTimeFormat(LOCALE, { ...options, timeZone: "UTC" })
      .format(value)
      .replace(".", "");
  return {
    weekday: format({ weekday: "short" }),
    day: String(day),
    month: format({ month: "short" }),
  };
}

/** Iniciales para mostrar cuando un profesional no tiene foto: "Juan Pérez" → "JP". */
export function initials(name: string): string {
  return name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((word) => word[0]!.toUpperCase())
    .join("");
}

/** Por ejemplo "vie 9 oct, 10:00": para listas donde entra poco texto. */
export function shortDateTime(instant: string, timeZone: string): string {
  const date = new Intl.DateTimeFormat(LOCALE, {
    weekday: "short",
    day: "numeric",
    month: "short",
    timeZone,
  })
    .format(new Date(instant))
    .replaceAll(".", "")
    .replace(",", "");
  return `${date}, ${time(instant, timeZone)}`;
}

/** Por ejemplo "viernes 25 de diciembre" para una fecha AAAA-MM-DD. */
export function longDate(date: string): string {
  const [year, month, day] = date.split("-").map(Number);
  return new Intl.DateTimeFormat(LOCALE, {
    weekday: "long",
    day: "numeric",
    month: "long",
    timeZone: "UTC",
  })
    .format(new Date(Date.UTC(year, month - 1, day, 12)))
    .replace(",", "");
}
