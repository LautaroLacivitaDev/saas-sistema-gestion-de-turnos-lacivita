"use client";

import type { Slot } from "@/lib/api/types";
import { cn } from "@/lib/utils";
import { messages } from "@/messages/es-AR";

/** Horarios libres de un día, en botones grandes para tocar con el pulgar. */
export function SlotGrid({
  slots,
  loading,
  disabled,
  selected,
  onSelect,
}: {
  slots: Slot[] | undefined;
  loading: boolean;
  disabled?: boolean;
  selected?: string;
  onSelect: (slot: Slot) => void;
}) {
  const t = messages.booking;
  if (loading) {
    return <p className="py-4 text-muted-foreground">{t.loadingTimes}</p>;
  }
  if (!slots || slots.length === 0) {
    return <p className="py-4 text-muted-foreground">{t.noTimes}</p>;
  }
  return (
    <div role="radiogroup" aria-label={t.times} className="grid grid-cols-3 gap-2 min-[400px]:grid-cols-4 sm:grid-cols-6">
      {slots.map((slot) => {
        const active = slot.startsAt === selected;
        return (
          <button
            key={slot.startsAt}
            type="button"
            role="radio"
            aria-checked={active}
            disabled={disabled}
            onClick={() => onSelect(slot)}
            className={cn(
              "h-11 rounded-lg border text-base tabular-nums transition-colors disabled:opacity-50",
              active ? "border-primary bg-primary text-primary-foreground" : "hover:bg-muted",
            )}
          >
            {slot.localTime.slice(0, 5)}
          </button>
        );
      })}
    </div>
  );
}
