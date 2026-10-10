"use client";

import { useMutation } from "@tanstack/react-query";
import { PlusIcon, XIcon } from "lucide-react";
import { useState } from "react";

import { Notice } from "@/components/notice";
import { Button } from "@/components/ui/button";
import { errorMessage } from "@/lib/api/errors";
import type { DayOfWeek, Week } from "@/lib/api/types";
import { messages } from "@/messages/es-AR";

const t = messages.panel.hours;

const DAYS: DayOfWeek[] = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];
const DEFAULT_RANGE = { start: "09:00", end: "18:00" };

type Range = { start: string; end: string };
type Days = Record<DayOfWeek, Range[]>;

function toDays(week: Week): Days {
  const days = Object.fromEntries(DAYS.map((day) => [day, [] as Range[]])) as Days;
  for (const { day, ranges } of week.days) {
    days[day] = ranges.map(({ start, end }) => ({ start, end }));
  }
  return days;
}

function toWeek(days: Days): Week {
  return { days: DAYS.filter((day) => days[day].length > 0).map((day) => ({ day, ranges: days[day] })) };
}

/**
 * Horario semanal: cada día abierto o cerrado, con una o más franjas (por ejemplo, de 9 a 13 y de 14 a 18).
 * Los días cerrados no se mandan, como pide la API.
 */
export function HoursEditor({
  id,
  initial,
  onSave,
  disabled = false,
}: {
  id: string;
  initial: Week;
  onSave: (week: Week) => Promise<unknown>;
  disabled?: boolean;
}) {
  const [days, setDays] = useState(() => toDays(initial));
  const save = useMutation({ mutationFn: () => onSave(toWeek(days)) });
  const update = (day: DayOfWeek, ranges: Range[]) => {
    save.reset();
    setDays((current) => ({ ...current, [day]: ranges }));
  };
  const copyMonday = () => {
    save.reset();
    setDays((current) => {
      const next = { ...current };
      for (const day of DAYS) {
        if (day !== "MONDAY" && next[day].length > 0) {
          next[day] = current.MONDAY.map((range) => ({ ...range }));
        }
      }
      return next;
    });
  };

  return (
    <form
      className="flex flex-col gap-3"
      onSubmit={(event) => {
        event.preventDefault();
        save.mutate();
      }}
    >
      <ul className="flex flex-col divide-y rounded-xl border">
        {DAYS.map((day) => {
          const ranges = days[day];
          const open = ranges.length > 0;
          return (
            <li key={day} className="flex flex-col gap-2 px-4 py-3">
              <label className="flex min-h-11 items-center justify-between gap-3">
                <span className="font-medium">{t.days[day]}</span>
                <span className="flex items-center gap-2 text-sm text-muted-foreground">
                  {!open && t.closed}
                  <input
                    type="checkbox"
                    className="size-5"
                    checked={open}
                    disabled={disabled}
                    onChange={(event) => update(day, event.target.checked ? [{ ...DEFAULT_RANGE }] : [])}
                  />
                </span>
              </label>
              {ranges.map((range, index) => (
                <div key={index} className="flex items-end gap-2">
                  <TimeInput
                    id={`${id}-${day}-${index}-start`}
                    label={t.from}
                    value={range.start}
                    disabled={disabled}
                    onChange={(start) => update(day, ranges.map((r, i) => (i === index ? { ...r, start } : r)))}
                  />
                  <TimeInput
                    id={`${id}-${day}-${index}-end`}
                    label={t.to}
                    value={range.end}
                    disabled={disabled}
                    onChange={(end) => update(day, ranges.map((r, i) => (i === index ? { ...r, end } : r)))}
                  />
                  <Button
                    type="button"
                    size="icon-lg"
                    variant="ghost"
                    aria-label={t.removeRange}
                    disabled={disabled}
                    className="size-11 shrink-0"
                    onClick={() => update(day, ranges.filter((_, i) => i !== index))}
                  >
                    <XIcon aria-hidden />
                  </Button>
                </div>
              ))}
              {open && (
                <Button
                  type="button"
                  variant="ghost"
                  size="touch"
                  className="self-start"
                  disabled={disabled}
                  onClick={() => update(day, [...ranges, { start: ranges.at(-1)?.end ?? "14:00", end: "20:00" }])}
                >
                  <PlusIcon aria-hidden />
                  {t.addRange}
                </Button>
              )}
            </li>
          );
        })}
      </ul>
      {!disabled && days.MONDAY.length > 0 && (
        <Button type="button" variant="outline" size="touch" onClick={copyMonday}>
          {t.copyToAll}
        </Button>
      )}
      {save.isError && <Notice tone="error">{errorMessage(save.error)}</Notice>}
      {save.isSuccess && <Notice tone="success">{messages.panel.saved}</Notice>}
      {!disabled && (
        <Button type="submit" size="touch" disabled={save.isPending}>
          {save.isPending ? messages.panel.saving : t.save}
        </Button>
      )}
    </form>
  );
}

function TimeInput({
  id,
  label,
  value,
  disabled,
  onChange,
}: {
  id: string;
  label: string;
  value: string;
  disabled: boolean;
  onChange: (value: string) => void;
}) {
  return (
    <label htmlFor={id} className="flex min-w-0 flex-1 flex-col gap-1 text-sm">
      <span className="text-muted-foreground">{label}</span>
      <input
        id={id}
        type="time"
        step={300}
        required
        disabled={disabled}
        value={value}
        onChange={(event) => onChange(event.target.value)}
        className="h-11 min-w-0 rounded-lg border border-input bg-background px-3 text-base outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
      />
    </label>
  );
}
