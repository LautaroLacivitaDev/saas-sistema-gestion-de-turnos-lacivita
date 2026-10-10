"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { Field } from "@/components/field";
import { Notice } from "@/components/notice";
import { Button } from "@/components/ui/button";
import { statusTone } from "@/lib/agenda";
import { api } from "@/lib/api/client";
import { ApiError, errorMessage } from "@/lib/api/errors";
import type { CustomerDetail as Detail } from "@/lib/api/types";
import { dateTime, dayParts, localDate, money } from "@/lib/format";
import { panelKey, usePanel } from "@/lib/panel";
import { cn } from "@/lib/utils";
import { messages } from "@/messages/es-AR";

const t = messages.panel.customers;
const required = messages.errors.required;

const schema = z
  .object({
    name: z.string().trim().min(1, required).max(120),
    phone: z.string().trim().max(30),
    email: z.union([z.literal(""), z.string().trim().email(messages.errors.invalidEmail)]),
    notes: z.string().max(2000),
    preferences: z.string().max(500),
  })
  .refine((data) => data.phone !== "" || data.email !== "", {
    message: messages.errors.contactRequired,
    path: ["phone"],
  });
type CustomerForm = z.infer<typeof schema>;

/** Ficha de un cliente: contacto y notas editables, e historial de turnos. */
export function CustomerDetail({ customerId }: { customerId: string }) {
  const panel = usePanel();
  const key = panelKey(panel.businessId, "customer", customerId);
  const customer = useQuery({
    queryKey: key,
    queryFn: () => api<Detail>(`/api/businesses/${panel.businessId}/customers/${customerId}`),
  });

  return (
    <div className="flex flex-col gap-5">
      <Link href={`/panel/${panel.businessId}/clientes`} className="flex h-11 items-center self-start text-sm text-muted-foreground">
        ← {t.back}
      </Link>
      {customer.isPending ? (
        <p className="text-muted-foreground">{messages.panel.loading}</p>
      ) : customer.isError ? (
        <Notice tone="error">{errorMessage(customer.error)}</Notice>
      ) : (
        <Loaded customer={customer.data} queryKey={key} />
      )}
    </div>
  );
}

function Loaded({ customer, queryKey }: { customer: Detail; queryKey: unknown[] }) {
  const panel = usePanel();
  const client = useQueryClient();
  const timeZone = panel.branches[0]?.timeZone ?? "America/Argentina/Buenos_Aires";
  const form = useForm<CustomerForm>({
    resolver: zodResolver(schema),
    values: {
      name: customer.name,
      phone: customer.phone ?? "",
      email: customer.email ?? "",
      notes: customer.notes ?? "",
      preferences: customer.preferences ?? "",
    },
  });
  const save = useMutation({
    mutationFn: (data: CustomerForm) =>
      api<Detail>(`/api/businesses/${panel.businessId}/customers/${customer.id}`, {
        method: "PUT",
        body: {
          contact: { name: data.name, phone: data.phone || undefined, email: data.email || undefined },
          notes: data.notes,
          preferences: data.preferences,
        },
      }),
    onSuccess: (updated) => {
      client.setQueryData(queryKey, updated);
      void client.invalidateQueries({ queryKey: panelKey(panel.businessId, "customers") });
    },
    onError: (error) => {
      if (error instanceof ApiError && error.code === "customer_email_taken") {
        form.setError("email", { message: error.message });
      }
    },
  });
  const errors = form.formState.errors;
  const since = dayParts(localDate(new Date(customer.createdAt), timeZone));

  return (
    <>
      <header className="flex flex-col gap-1">
        <h1 className="text-2xl font-semibold tracking-tight">{customer.name}</h1>
        <p className="text-sm text-muted-foreground">
          {t.since(`${since.day} ${since.month}`)}
          {customer.hasAccount && ` · ${t.hasAccount}`}
        </p>
      </header>

      <form
        noValidate
        className="flex flex-col gap-4 rounded-xl border p-4"
        onSubmit={form.handleSubmit((data) => save.mutate(data))}
      >
        <h2 className="font-semibold">{t.contact}</h2>
        <Field id="customer-name" label={t.name} autoComplete="off" error={errors.name?.message} {...form.register("name")} />
        <div className="grid gap-4 sm:grid-cols-2">
          <Field
            id="customer-phone"
            label={t.phone}
            type="tel"
            inputMode="tel"
            autoComplete="off"
            error={errors.phone?.message}
            {...form.register("phone")}
          />
          <Field
            id="customer-email"
            label={t.email}
            type="email"
            inputMode="email"
            autoComplete="off"
            error={errors.email?.message}
            {...form.register("email")}
          />
        </div>
        <Field
          id="customer-preferences"
          label={t.preferences}
          hint={t.preferencesHint}
          autoComplete="off"
          error={errors.preferences?.message}
          {...form.register("preferences")}
        />
        <label className="flex flex-col gap-1.5">
          <span className="text-sm font-medium">{t.notes}</span>
          <textarea
            rows={4}
            className="rounded-lg border border-input bg-background px-3 py-2 text-base outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
            aria-describedby="customer-notes-hint"
            {...form.register("notes")}
          />
          <span id="customer-notes-hint" className="text-sm text-muted-foreground">
            {t.notesHint}
          </span>
        </label>
        {save.isError && !(save.error instanceof ApiError && save.error.code === "customer_email_taken") && (
          <Notice tone="error">{errorMessage(save.error)}</Notice>
        )}
        {save.isSuccess && !form.formState.isDirty && <Notice tone="success">{messages.panel.saved}</Notice>}
        <Button type="submit" size="touch" className="self-start" disabled={save.isPending}>
          {save.isPending ? messages.panel.saving : messages.panel.save}
        </Button>
      </form>

      <section aria-labelledby="historial" className="flex flex-col gap-2">
        <h2 id="historial" className="font-semibold">
          {t.history}
        </h2>
        {customer.history.length === 0 ? (
          <p className="text-muted-foreground">{t.noHistory}</p>
        ) : (
          <ul className="flex flex-col gap-2">
            {customer.history.map((appointment) => (
              <li
                key={appointment.id}
                className={cn("flex flex-col gap-0.5 rounded-lg border border-l-4 px-3 py-2", statusTone(appointment.status))}
              >
                <span className="text-sm font-medium first-letter:uppercase">
                  {dateTime(appointment.startsAt, appointment.timeZone)}
                </span>
                <span className="text-sm">
                  {appointment.lines.map((line) => line.serviceName).join(" + ")} · {appointment.barberName}
                </span>
                <span className="text-xs text-muted-foreground">
                  {messages.panel.agenda.status[appointment.status]} · {appointment.branchName} ·{" "}
                  {money(appointment.totalPrice)}
                </span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </>
  );
}
