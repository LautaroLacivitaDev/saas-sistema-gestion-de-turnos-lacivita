"use client";

import { cn } from "@/lib/utils";

/** Pestañas en una sola fila, con zonas de 44 px. Si no entran, se desplazan con el dedo. */
export function Segmented<T extends string>({
  label,
  value,
  options,
  onChange,
}: {
  label: string;
  value: T;
  options: { value: T; label: string }[];
  onChange: (value: T) => void;
}) {
  return (
    <div role="tablist" aria-label={label} className="-mx-4 flex gap-1 overflow-x-auto px-4 [scrollbar-width:none]">
      {options.map((option) => (
        <button
          key={option.value}
          type="button"
          role="tab"
          aria-selected={value === option.value}
          onClick={() => onChange(option.value)}
          className={cn(
            "h-11 shrink-0 rounded-full border px-4 text-sm",
            value === option.value ? "border-primary bg-primary text-primary-foreground" : "hover:bg-muted",
          )}
        >
          {option.label}
        </button>
      ))}
    </div>
  );
}
