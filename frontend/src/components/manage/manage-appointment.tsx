"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useSearchParams } from "next/navigation";
import { useState } from "react";

import { DayStrip } from "@/components/booking/day-strip";
import { SlotGrid } from "@/components/booking/slot-grid";
import { useAvailability } from "@/components/booking/time-step";
import { Notice } from "@/components/notice";
import { Button, buttonVariants } from "@/components/ui/button";
import { api } from "@/lib/api/client";
import { ApiError, errorMessage } from "@/lib/api/errors";
import type { BookableItem, ManagedAppointment, Slot } from "@/lib/api/types";
import { dateTime, localDate, money } from "@/lib/format";
import { useNow } from "@/lib/use-now";
import { cn } from "@/lib/utils";
import { messages } from "@/messages/es-AR";

const t = messages.manage;

type Panel = "none" | "reschedule" | "cancel";

/** Panel que abre cada botón del email: "&accion=reprogramar" abre el de cambiar horario, etc. */
function panelFrom(action: string | null): Panel {
  return action === "reprogramar" ? "reschedule" : action === "cancelar" ? "cancel" : "none";
}

/**
 * Lo que el cliente hace con el link de su email, sin iniciar sesión: ver el turno, confirmar que va,
 * cambiarlo de horario o cancelarlo, dentro del plazo del negocio. El token viaja en el cuerpo de cada
 * pedido, nunca en la URL de la API.
 */
export function ManageAppointment() {
  const searchParams = useSearchParams();
  const token = searchParams.get("token") ?? "";
  const action = searchParams.get("accion");
  const client = useQueryClient();
  const now = useNow();
  const key = ["managed", token];

  const lookup = useQuery({
    queryKey: key,
    queryFn: () =>
      api<ManagedAppointment>("/api/public/appointments/lookup", { method: "POST", body: { token } }),
    enabled: token.length > 0,
    retry: false,
  });

  const [panel, setPanel] = useState<Panel>(panelFrom(action));
  const [done, setDone] = useState<string | null>(null);

  const change = useMutation({
    mutationFn: ({ path, body }: { path: string; body: object }) =>
      api<ManagedAppointment>(`/api/public/appointments/${path}`, { method: "POST", body: { token, ...body } }),
    onSuccess: (updated, { path }) => {
      client.setQueryData(key, updated);
      setPanel("none");
      setDone(path === "confirm" ? t.confirmed : path === "cancel" ? t.cancelled : t.rescheduled);
    },
  });

  if (!token || (lookup.isError && lookup.error instanceof ApiError && lookup.error.status === 404)) {
    return <Notice tone="error">{t.invalidLink}</Notice>;
  }
  if (lookup.isPending) {
    return <p className="text-muted-foreground">{t.loading}</p>;
  }
  if (lookup.isError) {
    return <Notice tone="error">{errorMessage(lookup.error)}</Notice>;
  }

  const { appointment, businessName, slug, changeableUntil } = lookup.data;
  const upcoming = appointment.status === "PENDING" || appointment.status === "CONFIRMED";
  const canChange = upcoming && new Date(changeableUntil).getTime() > now;
  const item: BookableItem = appointment.comboId
    ? { kind: "combo", id: appointment.comboId }
    : { kind: "service", id: appointment.lines[0]!.serviceId };

  return (
    <div className="flex flex-col gap-5">
      {done && <Notice tone="success">{done}</Notice>}
      {change.isError && <Notice tone="error">{errorMessage(change.error)}</Notice>}

      <section className="flex flex-col gap-2 rounded-xl border p-4" aria-label={t.title}>
        <div className="flex items-start justify-between gap-3">
          <p className="font-medium">{businessName}</p>
          <span
            className={cn(
              "shrink-0 rounded-full px-2.5 py-0.5 text-sm",
              appointment.status === "CANCELLED" ? "bg-destructive/10 text-destructive" : "bg-muted",
            )}
          >
            {t.status[appointment.status] ?? appointment.status}
          </span>
        </div>
        <p>
          {appointment.lines.map((line) => line.serviceName).join(" + ")}{" "}
          {messages.booking.with(appointment.barberName)}
        </p>
        <p className="first-letter:uppercase">{dateTime(appointment.startsAt, appointment.timeZone)}</p>
        <p className="text-sm text-muted-foreground">{appointment.branchName}</p>
        <p className="font-medium">{money(appointment.totalPrice)}</p>
      </section>

      {upcoming && (
        <p className="text-sm text-muted-foreground">
          {canChange ? t.changeableUntil(dateTime(changeableUntil, appointment.timeZone)) : t.tooLate}
        </p>
      )}

      {upcoming && (
        <div className="flex flex-col gap-2 sm:flex-row">
          {appointment.status === "PENDING" && (
            <Button
              size="touch"
              disabled={change.isPending}
              onClick={() => change.mutate({ path: "confirm", body: {} })}
            >
              {t.confirm}
            </Button>
          )}
          {canChange && (
            <>
              <Button
                size="touch"
                variant={panel === "reschedule" ? "secondary" : "outline"}
                aria-expanded={panel === "reschedule"}
                onClick={() => setPanel(panel === "reschedule" ? "none" : "reschedule")}
              >
                {t.reschedule}
              </Button>
              <Button
                size="touch"
                variant="destructive"
                aria-expanded={panel === "cancel"}
                onClick={() => setPanel(panel === "cancel" ? "none" : "cancel")}
              >
                {t.cancel}
              </Button>
            </>
          )}
        </div>
      )}

      {canChange && panel === "cancel" && (
        <section className="flex flex-col gap-3 rounded-xl border p-4" aria-label={t.cancel}>
          <p className="font-medium">{t.cancelQuestion}</p>
          <div className="flex flex-col gap-2 sm:flex-row">
            <Button
              size="touch"
              variant="destructive"
              disabled={change.isPending}
              onClick={() => change.mutate({ path: "cancel", body: {} })}
            >
              {t.cancelYes}
            </Button>
            <Button size="touch" variant="outline" onClick={() => setPanel("none")}>
              {t.cancelNo}
            </Button>
          </div>
        </section>
      )}

      {canChange && panel === "reschedule" && (
        <Reschedule
          slug={slug}
          branchId={appointment.branchId}
          barberId={appointment.barberId}
          timeZone={appointment.timeZone}
          item={item}
          current={appointment.startsAt}
          saving={change.isPending}
          onChoose={(slot) => change.mutate({ path: "reschedule", body: { startsAt: slot.startsAt } })}
        />
      )}

      {!upcoming && (
        <Link href={`/${slug}`} className={buttonVariants({ size: "touch", className: "self-start" })}>
          {t.bookAgain}
        </Link>
      )}
    </div>
  );
}

/** Horarios libres del mismo profesional para mover el turno. */
function Reschedule({
  slug,
  branchId,
  barberId,
  timeZone,
  item,
  current,
  saving,
  onChoose,
}: {
  slug: string;
  branchId: string;
  barberId: string;
  timeZone: string;
  item: BookableItem;
  current: string;
  saving: boolean;
  onChoose: (slot: Slot) => void;
}) {
  const today = localDate(new Date(), timeZone);
  const [date, setDate] = useState(() => {
    const day = localDate(new Date(current), timeZone);
    return day < today ? today : day;
  });
  const [chosen, setChosen] = useState<Slot | null>(null);
  const availability = useAvailability(slug, branchId, item, date, barberId);
  const slots = availability.data?.slots.filter((slot) => slot.startsAt !== current);

  return (
    <section className="flex flex-col gap-3 rounded-xl border p-4" aria-labelledby="nuevo-horario">
      <h2 id="nuevo-horario" className="font-medium">
        {t.rescheduleTitle}
      </h2>
      <DayStrip
        today={today}
        selected={date}
        onSelect={(day) => {
          setDate(day);
          setChosen(null);
        }}
      />
      {availability.isError ? (
        <Notice tone="error">{errorMessage(availability.error)}</Notice>
      ) : (
        <SlotGrid
          slots={slots}
          loading={availability.isPending}
          selected={chosen?.startsAt}
          onSelect={setChosen}
        />
      )}
      <Button size="touch" disabled={!chosen || saving} onClick={() => chosen && onChoose(chosen)}>
        {t.rescheduleConfirm}
      </Button>
    </section>
  );
}
