"use client";

import { useQuery } from "@tanstack/react-query";
import { ChevronLeftIcon, ChevronRightIcon, PlusIcon } from "lucide-react";
import { useState } from "react";

import { Notice } from "@/components/notice";
import { SelectField } from "@/components/select-field";
import { Button } from "@/components/ui/button";
import { daysOf, periodOf, shift, statusTone, type View } from "@/lib/agenda";
import { api } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import type { Appointment } from "@/lib/api/types";
import { dayParts, localDate, time } from "@/lib/format";
import { panelKey, usePanel } from "@/lib/panel";
import { TABLET_UP, useMedia } from "@/lib/use-media";
import { useNow } from "@/lib/use-now";
import { cn } from "@/lib/utils";
import { messages } from "@/messages/es-AR";

import { AppointmentSheet } from "./appointment-sheet";
import { NewAppointmentSheet } from "./new-appointment-sheet";

const t = messages.panel.agenda;

const DEFAULT_ZONE = "America/Argentina/Buenos_Aires";

/**
 * Agenda del equipo. "Día / Semana" en cualquier pantalla: por defecto Día en el celular y Semana desde
 * tablet. Se filtra por sucursal y por profesional; cada turno se toca para ver su ficha.
 */
export function Agenda() {
  const panel = usePanel();
  const wide = useMedia(TABLET_UP);
  const [chosenView, setChosenView] = useState<View | null>(null);
  const view = chosenView ?? (wide ? "week" : "day");
  const [branchId, setBranchId] = useState<string>("");
  const [barberId, setBarberId] = useState<string>("");
  const branch = panel.branches.find((candidate) => candidate.id === branchId);
  const timeZone = branch?.timeZone ?? panel.branches[0]?.timeZone ?? DEFAULT_ZONE;
  const now = useNow();
  const today = localDate(new Date(now), timeZone);
  const [date, setDate] = useState(today);
  const [selected, setSelected] = useState<Appointment | null>(null);
  const [creating, setCreating] = useState(false);

  const period = periodOf(view, date, timeZone);
  const appointments = useQuery({
    queryKey: panelKey(panel.businessId, "appointments", period.from, period.to, branchId, barberId),
    queryFn: () => {
      const params = new URLSearchParams(period);
      if (branchId) params.set("branchId", branchId);
      if (barberId) params.set("barberId", barberId);
      return api<Appointment[]>(`/api/businesses/${panel.businessId}/appointments?${params}`);
    },
    refetchInterval: 60_000,
  });

  const days = daysOf(view, date);
  const byDay = new Map(days.map((day) => [day, [] as Appointment[]]));
  for (const appointment of appointments.data ?? []) {
    byDay.get(localDate(new Date(appointment.startsAt), appointment.timeZone))?.push(appointment);
  }
  const professionals = branchId
    ? panel.professionals.filter((p) => p.branchIds.length === 0 || p.branchIds.includes(branchId))
    : panel.professionals;

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-2">
        <h1 className="text-2xl font-semibold tracking-tight">{t.title}</h1>
        <div role="radiogroup" aria-label={t.title} className="flex rounded-lg border p-0.5">
          {(["day", "week"] as const).map((option) => (
            <button
              key={option}
              type="button"
              role="radio"
              aria-checked={view === option}
              onClick={() => setChosenView(option)}
              className={cn(
                "h-10 rounded-md px-4 text-sm",
                view === option ? "bg-primary text-primary-foreground" : "text-muted-foreground",
              )}
            >
              {option === "day" ? t.day : t.week}
            </button>
          ))}
        </div>
      </div>

      <div className="flex items-center gap-2">
        <Button variant="outline" size="icon-lg" className="size-11" aria-label={t.previous} onClick={() => setDate(shift(view, date, -1))}>
          <ChevronLeftIcon aria-hidden />
        </Button>
        <Button variant="outline" size="touch" onClick={() => setDate(today)} disabled={date === today}>
          {t.today}
        </Button>
        <Button variant="outline" size="icon-lg" className="size-11" aria-label={t.next} onClick={() => setDate(shift(view, date, 1))}>
          <ChevronRightIcon aria-hidden />
        </Button>
        <p className="min-w-0 flex-1 truncate text-right text-sm text-muted-foreground first-letter:uppercase">
          {periodLabel(days)}
        </p>
      </div>

      <div className="grid grid-cols-2 gap-2 sm:flex sm:flex-wrap">
        {panel.branches.length > 1 && (
          <SelectField label={t.branch} value={branchId} onChange={(event) => setBranchId(event.target.value)}>
            <option value="">{messages.panel.allBranches}</option>
            {panel.branches.map((option) => (
              <option key={option.id} value={option.id}>
                {option.name}
              </option>
            ))}
          </SelectField>
        )}
        {panel.can("MANAGER") || professionals.length > 1 ? (
          <SelectField label={t.professional} value={barberId} onChange={(event) => setBarberId(event.target.value)}>
            <option value="">{messages.panel.everyone}</option>
            {professionals.map((option) => (
              <option key={option.barberId} value={option.barberId}>
                {option.barberId === panel.userId ? `${option.name} (${t.onlyMine.toLowerCase()})` : option.name}
              </option>
            ))}
          </SelectField>
        ) : null}
      </div>

      {appointments.isError && <Notice tone="error">{errorMessage(appointments.error)}</Notice>}

      <div className={cn("grid gap-3", view === "week" && "md:grid-cols-7 md:gap-2")}>
        {days.map((day) => (
          <section key={day} aria-label={dayLabel(day)} className="flex min-w-0 flex-col gap-2">
            {view === "week" && (
              <h2 className={cn("text-sm font-medium capitalize", day === today && "text-primary underline underline-offset-4")}>
                {dayLabel(day)}
              </h2>
            )}
            {appointments.isPending ? (
              <p className="text-sm text-muted-foreground">{messages.panel.loading}</p>
            ) : (byDay.get(day) ?? []).length === 0 ? (
              <p className="text-sm text-muted-foreground">{t.empty}</p>
            ) : (
              (byDay.get(day) ?? []).map((appointment) => (
                <AppointmentCard
                  key={appointment.id}
                  appointment={appointment}
                  compact={view === "week"}
                  showBranch={!branchId && panel.branches.length > 1}
                  onOpen={() => setSelected(appointment)}
                />
              ))
            )}
          </section>
        ))}
      </div>

      <Button
        size="touch"
        className="fixed right-4 bottom-[calc(5rem+env(safe-area-inset-bottom))] z-10 shadow-lg md:static md:self-start md:shadow-none"
        onClick={() => setCreating(true)}
      >
        <PlusIcon aria-hidden />
        {t.newAppointment}
      </Button>

      <AppointmentSheet appointment={selected} onClose={() => setSelected(null)} onChanged={setSelected} />
      <NewAppointmentSheet
        open={creating}
        onOpenChange={setCreating}
        defaultDate={date}
        defaultBranchId={branchId || (panel.branches.length === 1 ? panel.branches[0]!.id : "")}
      />
    </div>
  );
}

function AppointmentCard({
  appointment,
  compact,
  showBranch,
  onOpen,
}: {
  appointment: Appointment;
  compact: boolean;
  showBranch: boolean;
  onOpen: () => void;
}) {
  const services = appointment.lines.map((line) => line.serviceName).join(" + ");
  return (
    <button
      type="button"
      onClick={onOpen}
      className={cn(
        "flex min-h-14 w-full flex-col items-start gap-0.5 rounded-lg border border-l-4 px-3 py-2 text-left",
        statusTone(appointment.status),
      )}
    >
      <span className="text-sm font-medium tabular-nums">
        {time(appointment.startsAt, appointment.timeZone)}–{time(appointment.endsAt, appointment.timeZone)}
        {!compact && <span className="ml-2 font-normal">{t.status[appointment.status]}</span>}
      </span>
      <span className={cn("w-full truncate", compact ? "text-xs" : "text-base")}>
        {appointment.customer?.name ?? services}
      </span>
      <span className="w-full truncate text-xs text-muted-foreground">
        {compact ? appointment.barberName : `${services} · ${appointment.barberName}`}
        {showBranch && !compact ? ` · ${appointment.branchName}` : ""}
      </span>
    </button>
  );
}

function dayLabel(day: string): string {
  const parts = dayParts(day);
  return `${parts.weekday} ${parts.day}`;
}

function periodLabel(days: string[]): string {
  const first = dayParts(days[0]!);
  if (days.length === 1) {
    return `${first.weekday} ${first.day} ${first.month}`;
  }
  const last = dayParts(days[days.length - 1]!);
  return `${first.day} ${first.month} – ${last.day} ${last.month}`;
}
