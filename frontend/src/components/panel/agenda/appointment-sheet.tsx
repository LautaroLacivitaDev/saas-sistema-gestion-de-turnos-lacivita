"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { MailIcon, PhoneIcon } from "lucide-react";
import { useState } from "react";

import { Field } from "@/components/field";
import { Notice } from "@/components/notice";
import { SelectField } from "@/components/select-field";
import { Button, buttonVariants } from "@/components/ui/button";
import { Sheet } from "@/components/ui/sheet";
import { actionsFor, canMove, statusTone, type Action } from "@/lib/agenda";
import { api } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import type { Appointment, Delivery } from "@/lib/api/types";
import { dateTime, localDate, money, time, zonedInstant } from "@/lib/format";
import { panelKey, usePanel } from "@/lib/panel";
import { useNow } from "@/lib/use-now";
import { cn } from "@/lib/utils";
import { messages } from "@/messages/es-AR";

const t = messages.panel.agenda;

/** Ficha de un turno: datos, contacto del cliente, cambios de estado, mover y avisos enviados. */
export function AppointmentSheet({
  appointment,
  onClose,
  onChanged,
}: {
  appointment: Appointment | null;
  onClose: () => void;
  onChanged: (appointment: Appointment) => void;
}) {
  return (
    <Sheet open={appointment !== null} onOpenChange={(open) => !open && onClose()} title={t.title}>
      {appointment && <Details key={appointment.id} appointment={appointment} onChanged={onChanged} />}
    </Sheet>
  );
}

function Details({ appointment, onChanged }: { appointment: Appointment; onChanged: (a: Appointment) => void }) {
  const panel = usePanel();
  const client = useQueryClient();
  const now = useNow();
  const [confirmingCancel, setConfirmingCancel] = useState(false);
  const [moving, setMoving] = useState(false);
  const base = `/api/businesses/${panel.businessId}/appointments/${appointment.id}`;

  const refresh = (updated: Appointment) => {
    onChanged(updated);
    setConfirmingCancel(false);
    setMoving(false);
    void client.invalidateQueries({ queryKey: panelKey(panel.businessId, "appointments") });
    void client.invalidateQueries({ queryKey: panelKey(panel.businessId, "deliveries", appointment.id) });
  };
  const status = useMutation({
    mutationFn: (target: Action) => api<Appointment>(`${base}/status`, { method: "PUT", body: { status: target } }),
    onSuccess: refresh,
  });

  const services = appointment.lines.map((line) => line.serviceName).join(" + ");
  const actions = actionsFor(appointment.status, appointment.startsAt, now);

  return (
    <div className="flex flex-col gap-4">
      <div className={cn("flex flex-col gap-1 rounded-lg border border-l-4 p-3", statusTone(appointment.status))}>
        <p className="text-sm font-medium">{t.status[appointment.status]}</p>
        <p className="text-lg font-semibold first-letter:uppercase">
          {dateTime(appointment.startsAt, appointment.timeZone)}
        </p>
        <p>
          {services} · {appointment.barberName}
        </p>
        <p className="text-sm text-muted-foreground">
          {appointment.branchName} · {money(appointment.totalPrice)} · {time(appointment.startsAt, appointment.timeZone)}–
          {time(appointment.endsAt, appointment.timeZone)}
        </p>
      </div>

      <section aria-label={t.customer} className="flex flex-col gap-2">
        <h3 className="text-sm font-medium text-muted-foreground">{t.customer}</h3>
        {appointment.customer ? (
          <>
            <p className="font-medium">{appointment.customer.name}</p>
            <div className="flex flex-wrap gap-2">
              {appointment.customer.phone && (
                <a href={`tel:${appointment.customer.phone}`} className={buttonVariants({ variant: "outline", size: "touch" })}>
                  <PhoneIcon aria-hidden />
                  {appointment.customer.phone}
                </a>
              )}
              {appointment.customer.email && (
                <a
                  href={`mailto:${appointment.customer.email}`}
                  className={buttonVariants({ variant: "outline", size: "touch", className: "min-w-0" })}
                >
                  <MailIcon aria-hidden />
                  <span className="truncate">{appointment.customer.email}</span>
                </a>
              )}
            </div>
          </>
        ) : (
          <p className="text-sm text-muted-foreground">{t.contactHidden}</p>
        )}
      </section>

      {status.isError && <Notice tone="error">{errorMessage(status.error)}</Notice>}

      {(actions.length > 0 || canMove(appointment.status)) && (
        <div className="grid grid-cols-2 gap-2">
          {actions
            .filter((action) => action !== "CANCELLED")
            .map((action) => (
              <Button key={action} size="touch" variant="secondary" disabled={status.isPending} onClick={() => status.mutate(action)}>
                {t.actions[action]}
              </Button>
            ))}
          {canMove(appointment.status) && (
            <Button size="touch" variant="outline" onClick={() => setMoving(!moving)} aria-expanded={moving}>
              {t.move}
            </Button>
          )}
          {actions.includes("CANCELLED") && (
            <Button size="touch" variant="destructive" onClick={() => setConfirmingCancel(true)}>
              {t.actions.CANCELLED}
            </Button>
          )}
        </div>
      )}

      {confirmingCancel && (
        <div className="flex flex-col gap-2 rounded-lg border p-3">
          <p>{t.cancelQuestion}</p>
          <div className="flex gap-2">
            <Button size="touch" variant="destructive" disabled={status.isPending} onClick={() => status.mutate("CANCELLED")}>
              {messages.manage.cancelYes}
            </Button>
            <Button size="touch" variant="outline" onClick={() => setConfirmingCancel(false)}>
              {messages.manage.cancelNo}
            </Button>
          </div>
        </div>
      )}

      {moving && <MoveForm appointment={appointment} base={base} onMoved={refresh} />}

      <Deliveries appointmentId={appointment.id} />
    </div>
  );
}

/** Mover a otro día, hora o profesional de la misma sucursal. Conserva servicios y precio. */
function MoveForm({
  appointment,
  base,
  onMoved,
}: {
  appointment: Appointment;
  base: string;
  onMoved: (appointment: Appointment) => void;
}) {
  const panel = usePanel();
  const [date, setDate] = useState(localDate(new Date(appointment.startsAt), appointment.timeZone));
  const [hhmm, setHhmm] = useState(time(appointment.startsAt, appointment.timeZone));
  const [barberId, setBarberId] = useState(appointment.barberId);
  const candidates = panel.professionals.filter(
    (professional) => professional.branchIds.length === 0 || professional.branchIds.includes(appointment.branchId),
  );
  const move = useMutation({
    mutationFn: () =>
      api<Appointment>(`${base}/time`, {
        method: "PUT",
        body: { startsAt: zonedInstant(date, hhmm, appointment.timeZone), barberId },
      }),
    onSuccess: onMoved,
  });
  return (
    <form
      className="flex flex-col gap-3 rounded-lg border p-3"
      onSubmit={(event) => {
        event.preventDefault();
        move.mutate();
      }}
    >
      <h3 className="font-medium">{t.moveTitle}</h3>
      <div className="grid grid-cols-2 gap-2">
        <Field id="move-date" label={t.date} type="date" value={date} onChange={(event) => setDate(event.target.value)} required />
        <Field id="move-time" label={t.time} type="time" value={hhmm} onChange={(event) => setHhmm(event.target.value)} required />
      </div>
      <SelectField label={t.professional} value={barberId} onChange={(event) => setBarberId(event.target.value)}>
        {candidates.map((professional) => (
          <option key={professional.barberId} value={professional.barberId}>
            {professional.name}
          </option>
        ))}
      </SelectField>
      {move.isError && <Notice tone="error">{errorMessage(move.error)}</Notice>}
      <Button type="submit" size="touch" disabled={move.isPending}>
        {move.isPending ? messages.panel.saving : t.move}
      </Button>
    </form>
  );
}

/** Registro de envíos del turno: qué avisos salieron, cuáles esperan y cuáles fallaron. */
function Deliveries({ appointmentId }: { appointmentId: string }) {
  const panel = usePanel();
  const deliveries = useQuery({
    queryKey: panelKey(panel.businessId, "deliveries", appointmentId),
    queryFn: () =>
      api<Delivery[]>(`/api/businesses/${panel.businessId}/appointments/${appointmentId}/notifications`),
  });
  const timeZone = panel.branches[0]?.timeZone ?? "America/Argentina/Buenos_Aires";
  return (
    <section aria-label={t.deliveries} className="flex flex-col gap-2">
      <h3 className="text-sm font-medium text-muted-foreground">{t.deliveries}</h3>
      {deliveries.isPending ? (
        <p className="text-sm text-muted-foreground">{messages.panel.loading}</p>
      ) : deliveries.isError ? (
        <Notice tone="error">{errorMessage(deliveries.error)}</Notice>
      ) : deliveries.data.length === 0 ? (
        <p className="text-sm text-muted-foreground">{t.noDeliveries}</p>
      ) : (
        <ul className="divide-y rounded-lg border text-sm">
          {deliveries.data.map((delivery) => (
            <li key={delivery.id} className="flex items-center justify-between gap-3 px-3 py-2">
              <span className="min-w-0">
                {t.deliveryType[delivery.type] ?? delivery.type}{" "}
                {delivery.audience === "CUSTOMER" ? t.toCustomer : t.toProfessional}
                <span className="block text-xs text-muted-foreground first-letter:uppercase">
                  {dateTime(delivery.sentAt ?? delivery.dueAt, timeZone)}
                </span>
              </span>
              <span className="shrink-0">{t.deliveryStatus[delivery.status] ?? delivery.status}</span>
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}
