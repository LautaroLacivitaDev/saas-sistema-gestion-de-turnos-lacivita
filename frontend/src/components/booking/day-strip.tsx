"use client";

import { addDays, dayParts } from "@/lib/format";
import { cn } from "@/lib/utils";
import { messages } from "@/messages/es-AR";

/** Cuántos días se ofrecen para elegir. */
export const DAYS_AHEAD = 14;

/**
 * Tira de días para elegir, desplazable con el dedo. Empieza hoy (en la zona de la sucursal).
 */
export function DayStrip({
  today,
  selected,
  onSelect,
}: {
  today: string;
  selected: string;
  onSelect: (date: string) => void;
}) {
  const days = Array.from({ length: DAYS_AHEAD }, (_, index) => addDays(today, index));
  return (
    <div
      role="radiogroup"
      aria-label={messages.booking.day}
      className="-mx-4 flex snap-x scroll-px-4 gap-2 overflow-x-auto px-4 pb-1 [scrollbar-width:none]"
    >
      {days.map((date, index) => {
        const parts = dayParts(date);
        const label =
          index === 0 ? messages.booking.today : index === 1 ? messages.booking.tomorrow : parts.weekday;
        const active = date === selected;
        return (
          <button
            key={date}
            type="button"
            role="radio"
            aria-checked={active}
            aria-label={`${label} ${parts.day} ${parts.month}`}
            onClick={() => onSelect(date)}
            className={cn(
              "flex min-h-16 w-16 shrink-0 snap-start flex-col items-center justify-center rounded-xl border text-sm transition-colors",
              active ? "border-primary bg-primary text-primary-foreground" : "hover:bg-muted",
            )}
          >
            <span className="capitalize">{label}</span>
            <span className="text-lg font-semibold">{parts.day}</span>
          </button>
        );
      })}
    </div>
  );
}
