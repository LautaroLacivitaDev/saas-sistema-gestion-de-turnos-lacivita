"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { Field } from "@/components/field";
import { Notice } from "@/components/notice";
import { Button } from "@/components/ui/button";
import { Sheet } from "@/components/ui/sheet";
import { api } from "@/lib/api/client";
import { ApiError, errorMessage } from "@/lib/api/errors";
import type { TeamService } from "@/lib/api/types";
import { usePanel } from "@/lib/panel";
import { messages } from "@/messages/es-AR";

import { useRefreshCatalog } from "./catalog";

const t = messages.panel.catalog;
const required = messages.errors.required;

const schema = z.object({
  name: z.string().trim().min(1, required).max(80),
  category: z.string().trim().min(1, required).max(40),
  description: z.string().trim().max(500),
  baseDurationMinutes: z.coerce
    .number<string>()
    .int()
    .min(5, t.durationHint)
    .max(480, t.durationHint)
    .refine((minutes) => minutes % 5 === 0, t.durationHint),
  basePrice: z.coerce.number<string>().min(0, required),
});
type ServiceForm = z.input<typeof schema>;
type ServiceData = z.output<typeof schema>;

/** Crear, editar o proponer un servicio. El barbero propone; el gerente y el dueño crean y editan. */
export function ServiceSheet({ service, onClose }: { service: TeamService | "new" | null; onClose: () => void }) {
  const panel = usePanel();
  const title =
    service === "new" ? (panel.can("MANAGER") ? t.newService : t.proposeService) : t.editService;
  return (
    <Sheet open={service !== null} onOpenChange={(open) => !open && onClose()} title={title}>
      {service !== null && (
        <ServiceEditor key={service === "new" ? "new" : service.id} service={service === "new" ? null : service} onDone={onClose} />
      )}
    </Sheet>
  );
}

function ServiceEditor({ service, onDone }: { service: TeamService | null; onDone: () => void }) {
  const panel = usePanel();
  const refresh = useRefreshCatalog();
  const base = `/api/businesses/${panel.businessId}`;
  const form = useForm<ServiceForm, unknown, ServiceData>({
    resolver: zodResolver(schema),
    defaultValues: {
      name: service?.name ?? "",
      category: service?.category ?? "",
      description: service?.description ?? "",
      baseDurationMinutes: String(service?.baseDurationMinutes ?? 30),
      basePrice: service ? String(service.basePrice) : "",
    },
  });
  const save = useMutation({
    mutationFn: (data: ServiceData) => {
      const body = { ...data, description: data.description || undefined };
      if (service) {
        return api<TeamService>(`${base}/services/${service.id}`, { method: "PUT", body });
      }
      return api<TeamService>(panel.can("MANAGER") ? `${base}/services` : `${base}/service-proposals`, {
        method: "POST",
        body,
      });
    },
    onSuccess: () => {
      refresh();
      onDone();
    },
    onError: (error) => {
      if (error instanceof ApiError) {
        for (const [field, message] of Object.entries(error.fieldErrors)) {
          if (field in form.getValues()) {
            form.setError(field as keyof ServiceForm, { message });
          }
        }
      }
    },
  });
  const errors = form.formState.errors;

  return (
    <div className="flex flex-col gap-6">
      <form noValidate className="flex flex-col gap-4" onSubmit={form.handleSubmit((data) => save.mutate(data))}>
        {!service && !panel.can("MANAGER") && <Notice>{t.proposeHint}</Notice>}
        <Field id="service-name" label={t.name} autoComplete="off" error={errors.name?.message} {...form.register("name")} />
        <Field
          id="service-category"
          label={t.category}
          hint={t.categoryHint}
          autoComplete="off"
          error={errors.category?.message}
          {...form.register("category")}
        />
        <Field
          id="service-description"
          label={t.description}
          autoComplete="off"
          error={errors.description?.message}
          {...form.register("description")}
        />
        <div className="grid grid-cols-2 gap-3">
          <Field
            id="service-duration"
            label={t.duration}
            type="number"
            inputMode="numeric"
            step={5}
            min={5}
            max={480}
            error={errors.baseDurationMinutes?.message}
            {...form.register("baseDurationMinutes")}
          />
          <Field
            id="service-price"
            label={t.price}
            type="number"
            inputMode="decimal"
            min={0}
            error={errors.basePrice?.message}
            {...form.register("basePrice")}
          />
        </div>
        {save.isError && <Notice tone="error">{errorMessage(save.error)}</Notice>}
        <Button type="submit" size="touch" disabled={save.isPending}>
          {save.isPending ? messages.panel.saving : messages.panel.save}
        </Button>
      </form>
      {service && panel.can("OWNER") && <PriceRange service={service} />}
      {service && panel.can("MANAGER") && service.status !== "PROPOSED" && <Availability service={service} onDone={onDone} />}
    </div>
  );
}

/** Rango de precios que pueden fijar los profesionales para el servicio. Solo el dueño. */
function PriceRange({ service }: { service: TeamService }) {
  const panel = usePanel();
  const refresh = useRefreshCatalog();
  const [min, setMin] = useState(service.priceMin === null ? "" : String(service.priceMin));
  const [max, setMax] = useState(service.priceMax === null ? "" : String(service.priceMax));
  const save = useMutation({
    mutationFn: () =>
      api(`/api/businesses/${panel.businessId}/services/${service.id}/price-range`, {
        method: "PUT",
        body: { min: min === "" ? null : Number(min), max: max === "" ? null : Number(max) },
      }),
    onSuccess: refresh,
  });
  return (
    <form
      className="flex flex-col gap-3 border-t pt-4"
      onSubmit={(event) => {
        event.preventDefault();
        save.mutate();
      }}
    >
      <h3 className="font-medium">{t.range}</h3>
      <p className="text-sm text-muted-foreground">{t.rangeHint}</p>
      <div className="grid grid-cols-2 gap-3">
        <Field id="range-min" label={t.min} type="number" inputMode="decimal" min={0} value={min} onChange={(e) => setMin(e.target.value)} />
        <Field id="range-max" label={t.max} type="number" inputMode="decimal" min={0} value={max} onChange={(e) => setMax(e.target.value)} />
      </div>
      {save.isError && <Notice tone="error">{errorMessage(save.error)}</Notice>}
      {save.isSuccess && <Notice tone="success">{messages.panel.saved}</Notice>}
      <Button type="submit" size="touch" variant="outline" disabled={save.isPending}>
        {t.saveRange}
      </Button>
    </form>
  );
}

/** Pausar un servicio (deja de poder reservarse) o volver a ofrecerlo. */
function Availability({ service, onDone }: { service: TeamService; onDone: () => void }) {
  const panel = usePanel();
  const refresh = useRefreshCatalog();
  const active = service.status === "ACTIVE";
  const change = useMutation({
    mutationFn: () =>
      api(`/api/businesses/${panel.businessId}/services/${service.id}/active`, {
        method: "PUT",
        body: { active: !active },
      }),
    onSuccess: () => {
      refresh();
      onDone();
    },
  });
  return (
    <div className="flex flex-col gap-2 border-t pt-4">
      {change.isError && <Notice tone="error">{errorMessage(change.error)}</Notice>}
      <Button size="touch" variant={active ? "destructive" : "secondary"} disabled={change.isPending} onClick={() => change.mutate()}>
        {active ? t.deactivate : t.reactivate}
      </Button>
    </div>
  );
}
