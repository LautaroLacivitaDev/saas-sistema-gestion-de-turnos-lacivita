"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useForm, useWatch } from "react-hook-form";
import { z } from "zod";

import { Field } from "@/components/field";
import { Notice } from "@/components/notice";
import { SelectField } from "@/components/select-field";
import { Button } from "@/components/ui/button";
import { Sheet } from "@/components/ui/sheet";
import { api } from "@/lib/api/client";
import { ApiError, errorMessage } from "@/lib/api/errors";
import type { Appointment } from "@/lib/api/types";
import { worksAt } from "@/lib/booking";
import { money, zonedInstant } from "@/lib/format";
import { panelKey, usePanel } from "@/lib/panel";
import { messages } from "@/messages/es-AR";

const t = messages.panel.agenda;
const required = messages.errors.required;

const schema = z
  .object({
    branchId: z.string().min(1, required),
    barberId: z.string().min(1, required),
    item: z.string().min(1, required),
    date: z.string().min(1, required),
    time: z.string().min(1, required),
    name: z.string().trim().min(1, required).max(120),
    phone: z.string().trim().max(30),
    email: z.union([z.literal(""), z.string().trim().email(messages.errors.invalidEmail)]),
    confirmed: z.boolean(),
  })
  .refine((data) => data.phone !== "" || data.email !== "", {
    message: messages.errors.contactRequired,
    path: ["phone"],
  });
type NewAppointment = z.infer<typeof schema>;

/**
 * Turno de un cliente que llamó o llegó sin reserva. No se limita al horario de trabajo (lo decide el
 * equipo), pero nunca se superpone con otro turno del profesional.
 */
export function NewAppointmentSheet({
  open,
  onOpenChange,
  defaultDate,
  defaultBranchId,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  defaultDate: string;
  defaultBranchId: string;
}) {
  return (
    <Sheet open={open} onOpenChange={onOpenChange} title={t.newAppointment}>
      {open && (
        <NewAppointmentForm defaultDate={defaultDate} defaultBranchId={defaultBranchId} onDone={() => onOpenChange(false)} />
      )}
    </Sheet>
  );
}

function NewAppointmentForm({
  defaultDate,
  defaultBranchId,
  onDone,
}: {
  defaultDate: string;
  defaultBranchId: string;
  onDone: () => void;
}) {
  const panel = usePanel();
  const client = useQueryClient();
  const selfOffers = panel.professionals.some((professional) => professional.barberId === panel.userId);
  const form = useForm<NewAppointment>({
    resolver: zodResolver(schema),
    defaultValues: {
      branchId: defaultBranchId,
      barberId: !panel.can("MANAGER") && selfOffers ? panel.userId : "",
      item: "",
      date: defaultDate,
      time: "",
      name: "",
      phone: "",
      email: "",
      confirmed: true,
    },
  });
  const [branchId, barberId] = useWatch({ control: form.control, name: ["branchId", "barberId"] });
  const branch = panel.branches.find((candidate) => candidate.id === branchId);
  const candidates = branchId ? panel.professionals.filter((professional) => worksAt(professional, branchId)) : [];
  const offers = [
    ...panel.catalog.services.flatMap((service) => {
      const terms = service.barbers.find((barber) => barber.barberId === barberId);
      return terms ? [{ value: `service:${service.id}`, label: `${service.name} · ${money(terms.price)}` }] : [];
    }),
    ...panel.catalog.combos.flatMap((combo) => {
      const terms = combo.barbers.find((barber) => barber.barberId === barberId);
      return terms ? [{ value: `combo:${combo.id}`, label: `${combo.name} · ${money(terms.price)}` }] : [];
    }),
  ];

  const book = useMutation({
    mutationFn: (data: NewAppointment) => {
      const [kind, id] = data.item.split(":");
      return api<Appointment>(`/api/businesses/${panel.businessId}/appointments`, {
        method: "POST",
        body: {
          branchId: data.branchId,
          barberId: data.barberId,
          [kind === "combo" ? "comboId" : "serviceId"]: id,
          startsAt: zonedInstant(data.date, data.time, branch!.timeZone),
          confirmed: data.confirmed,
          customer: { name: data.name, phone: data.phone || undefined, email: data.email || undefined },
        },
      });
    },
    onSuccess: () => {
      void client.invalidateQueries({ queryKey: panelKey(panel.businessId, "appointments") });
      onDone();
    },
    onError: (error) => {
      if (error instanceof ApiError) {
        for (const [field, message] of Object.entries(error.fieldErrors)) {
          const name = field.replace("customer.", "") as keyof NewAppointment;
          if (name in form.getValues()) {
            form.setError(name, { message });
          }
        }
      }
    },
  });
  const errors = form.formState.errors;

  return (
    <form noValidate className="flex flex-col gap-4" onSubmit={form.handleSubmit((data) => book.mutate(data))}>
      {panel.branches.length > 1 && (
        <SelectField label={t.branch} {...form.register("branchId")}>
          <option value="">{t.form.choose}</option>
          {panel.branches.map((option) => (
            <option key={option.id} value={option.id}>
              {option.name}
            </option>
          ))}
        </SelectField>
      )}
      <SelectField label={t.professional} {...form.register("barberId")}>
        <option value="">{t.form.choose}</option>
        {candidates.map((professional) => (
          <option key={professional.barberId} value={professional.barberId}>
            {professional.name}
          </option>
        ))}
      </SelectField>
      <SelectField label={t.form.service} {...form.register("item")} disabled={!barberId}>
        <option value="">{t.form.choose}</option>
        {offers.map((offer) => (
          <option key={offer.value} value={offer.value}>
            {offer.label}
          </option>
        ))}
      </SelectField>
      <div className="grid grid-cols-2 gap-2">
        <Field id="new-date" label={t.date} type="date" error={errors.date?.message} {...form.register("date")} />
        <Field id="new-time" label={t.time} type="time" error={errors.time?.message} {...form.register("time")} />
      </div>
      <Field id="new-name" label={t.form.name} autoComplete="off" error={errors.name?.message} {...form.register("name")} />
      <Field
        id="new-phone"
        label={t.form.phone}
        type="tel"
        inputMode="tel"
        autoComplete="off"
        error={errors.phone?.message}
        {...form.register("phone")}
      />
      <Field
        id="new-email"
        label={t.form.email}
        type="email"
        inputMode="email"
        autoComplete="off"
        hint={t.form.contactHint}
        error={errors.email?.message}
        {...form.register("email")}
      />
      <label className="flex min-h-11 items-start gap-3">
        <input type="checkbox" className="mt-1 size-5" {...form.register("confirmed")} />
        <span className="flex flex-col">
          <span>{t.form.confirmed}</span>
          <span className="text-sm text-muted-foreground">{t.form.pendingHint}</span>
        </span>
      </label>
      {book.isError && <Notice tone="error">{errorMessage(book.error)}</Notice>}
      <Button type="submit" size="touch" disabled={book.isPending}>
        {book.isPending ? messages.panel.saving : t.form.save}
      </Button>
    </form>
  );
}
