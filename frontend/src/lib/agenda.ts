import { addDays, zonedInstant } from "@/lib/format";

// Reglas de la agenda del panel que no dependen de la pantalla. Las acciones reflejan las del backend
// (Appointment): el backend es quien decide; acá solo se ofrecen las que tienen sentido.

export type View = "day" | "week";

/** Estado nuevo al que puede pasar un turno desde la agenda. */
export type Action = "CONFIRMED" | "IN_PROGRESS" | "COMPLETED" | "NO_SHOW" | "CANCELLED";

/**
 * Lo que se puede hacer con un turno según su estado y si ya llegó su hora: confirmar uno "a confirmar",
 * empezarlo, terminarlo o marcar que no vino (solo después de la hora), o cancelarlo.
 */
export function actionsFor(status: string, startsAt: string, now: number): Action[] {
  const started = new Date(startsAt).getTime() <= now;
  switch (status) {
    case "PENDING":
      return started ? ["CONFIRMED", "NO_SHOW", "CANCELLED"] : ["CONFIRMED", "CANCELLED"];
    case "CONFIRMED":
      return started ? ["IN_PROGRESS", "COMPLETED", "NO_SHOW", "CANCELLED"] : ["IN_PROGRESS", "CANCELLED"];
    case "IN_PROGRESS":
      return started ? ["COMPLETED"] : [];
    default:
      return [];
  }
}

/** Se puede mover mientras está a confirmar o confirmado. */
export function canMove(status: string): boolean {
  return status === "PENDING" || status === "CONFIRMED";
}

/** Colores por estado: la agenda se lee de un vistazo. */
export function statusTone(status: string): string {
  switch (status) {
    case "PENDING":
      return "border-l-amber-500 bg-amber-50 dark:bg-amber-950/40";
    case "CONFIRMED":
      return "border-l-sky-500 bg-sky-50 dark:bg-sky-950/40";
    case "IN_PROGRESS":
      return "border-l-violet-500 bg-violet-50 dark:bg-violet-950/40";
    case "COMPLETED":
      return "border-l-emerald-500 bg-emerald-50 dark:bg-emerald-950/40";
    case "NO_SHOW":
      return "border-l-rose-500 bg-rose-50 dark:bg-rose-950/40";
    default:
      return "border-l-muted-foreground/40 bg-muted/40 text-muted-foreground line-through";
  }
}

/** El lunes de la semana de esa fecha (AAAA-MM-DD). */
export function weekStart(date: string): string {
  const [year, month, day] = date.split("-").map(Number);
  const weekday = new Date(Date.UTC(year, month - 1, day)).getUTCDay(); // 0 = domingo
  return addDays(date, weekday === 0 ? -6 : 1 - weekday);
}

/** Los días que muestra la vista: uno, o de lunes a domingo. */
export function daysOf(view: View, date: string): string[] {
  if (view === "day") {
    return [date];
  }
  const monday = weekStart(date);
  return Array.from({ length: 7 }, (_, index) => addDays(monday, index));
}

/** Desde y hasta (instantes ISO) para pedir los turnos de la vista, en la zona de la sucursal. */
export function periodOf(view: View, date: string, timeZone: string): { from: string; to: string } {
  const days = daysOf(view, date);
  return {
    from: zonedInstant(days[0]!, "00:00", timeZone),
    to: zonedInstant(addDays(days[days.length - 1]!, 1), "00:00", timeZone),
  };
}

/** Avanza o retrocede un día o una semana. */
export function shift(view: View, date: string, direction: 1 | -1): string {
  return addDays(date, (view === "day" ? 1 : 7) * direction);
}
