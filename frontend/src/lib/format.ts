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
  const part = (type: string) => parts.find((p) => p.type === type)?.value ?? "";
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
  const part = (type: string) => parts.find((p) => p.type === type)?.value ?? "";
  return `${part("year")}-${part("month")}-${part("day")}`;
}

/** Suma días a una fecha AAAA-MM-DD, sin zonas horarias de por medio. */
export function addDays(date: string, days: number): string {
  const [year, month, day] = date.split("-").map(Number);
  const result = new Date(Date.UTC(year, month - 1, day + days));
  return result.toISOString().slice(0, 10);
}

/** Por ejemplo { weekday: "vie", day: "10", month: "oct" } para los botones de días. */
export function dayParts(date: string): { weekday: string; day: string; month: string } {
  const [year, month, day] = date.split("-").map(Number);
  const value = new Date(Date.UTC(year, month - 1, day, 12));
  const format = (options: Intl.DateTimeFormatOptions) =>
    new Intl.DateTimeFormat(LOCALE, { ...options, timeZone: "UTC" }).format(value).replace(".", "");
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
