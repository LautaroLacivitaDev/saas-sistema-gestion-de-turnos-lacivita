"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";

import { Field } from "@/components/field";
import { Notice } from "@/components/notice";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import type { Offering, TeamService } from "@/lib/api/types";
import { money } from "@/lib/format";
import { panelKey, usePanel } from "@/lib/panel";
import { messages } from "@/messages/es-AR";

import { useRefreshCatalog, useTeamServices } from "../catalog/catalog";

const t = messages.panel.profile;

/**
 * Servicios que hace un profesional, con su precio y su duración propios (vacío: los base del negocio).
 * Un precio fuera del rango del dueño queda pendiente hasta que lo apruebe el gerente.
 */
export function Offerings({ barberId }: { barberId: string }) {
  const panel = usePanel();
  const services = useTeamServices();
  const offerings = useQuery({
    queryKey: panelKey(panel.businessId, "offerings", barberId),
    queryFn: () => api<Offering[]>(`/api/businesses/${panel.businessId}/barbers/${barberId}/services`),
  });
  if (services.isPending || offerings.isPending) {
    return <p className="text-muted-foreground">{messages.panel.loading}</p>;
  }
  if (services.isError || offerings.isError) {
    return <Notice tone="error">{errorMessage(services.error ?? offerings.error)}</Notice>;
  }
  const byService = new Map(offerings.data.map((offering) => [offering.serviceId, offering]));
  const active = (services.data ?? []).filter((service) => service.status === "ACTIVE");
  return (
    <ul className="flex flex-col gap-2">
      {active.map((service) => (
        <OfferingRow key={service.id} barberId={barberId} service={service} offering={byService.get(service.id) ?? null} />
      ))}
    </ul>
  );
}

function OfferingRow({
  barberId,
  service,
  offering,
}: {
  barberId: string;
  service: TeamService;
  offering: Offering | null;
}) {
  const panel = usePanel();
  const client = useQueryClient();
  const refreshCatalog = useRefreshCatalog();
  const [price, setPrice] = useState(offering?.ownPrice === null || !offering ? "" : String(offering.ownPrice));
  const [duration, setDuration] = useState(
    offering?.ownDurationMinutes === null || !offering ? "" : String(offering.ownDurationMinutes),
  );
  const url = `/api/businesses/${panel.businessId}/barbers/${barberId}/services/${service.id}`;
  const refresh = () => {
    void client.invalidateQueries({ queryKey: panelKey(panel.businessId, "offerings", barberId) });
    refreshCatalog();
  };
  const save = useMutation({
    mutationFn: () =>
      api<{ outcome: string }>(url, {
        method: "PUT",
        body: {
          price: price === "" ? null : Number(price),
          durationMinutes: duration === "" ? null : Number(duration),
        },
      }),
    onSuccess: refresh,
  });
  const withdraw = useMutation({ mutationFn: () => api(url, { method: "DELETE" }), onSuccess: refresh });

  return (
    <li className="flex flex-col gap-3 rounded-xl border p-4">
      <div className="flex items-start justify-between gap-3">
        <span className="flex flex-col">
          <span className="font-medium">{service.name}</span>
          <span className="text-sm text-muted-foreground">{t.base(money(service.basePrice), service.baseDurationMinutes)}</span>
        </span>
        {!offering && <span className="text-sm text-muted-foreground">{t.notOffered}</span>}
      </div>
      {offering ? (
        <form
          className="flex flex-col gap-3"
          onSubmit={(event) => {
            event.preventDefault();
            save.mutate();
          }}
        >
          <div className="grid grid-cols-2 gap-3">
            <Field
              id={`price-${service.id}`}
              label={t.ownPrice}
              type="number"
              inputMode="decimal"
              min={0}
              placeholder={String(service.basePrice)}
              value={price}
              onChange={(event) => setPrice(event.target.value)}
            />
            <Field
              id={`duration-${service.id}`}
              label={t.ownDuration}
              type="number"
              inputMode="numeric"
              step={5}
              min={5}
              placeholder={String(service.baseDurationMinutes)}
              value={duration}
              onChange={(event) => setDuration(event.target.value)}
            />
          </div>
          {offering.requestedPrice !== null && <Notice>{t.awaitingApproval(money(offering.requestedPrice))}</Notice>}
          {save.isSuccess && <Notice tone="success">{t.outcome[save.data.outcome] ?? messages.panel.saved}</Notice>}
          {(save.isError || withdraw.isError) && <Notice tone="error">{errorMessage(save.error ?? withdraw.error)}</Notice>}
          <div className="flex flex-wrap gap-2">
            <Button type="submit" size="touch" disabled={save.isPending}>
              {messages.panel.save}
            </Button>
            <Button type="button" size="touch" variant="ghost" disabled={withdraw.isPending} onClick={() => withdraw.mutate()}>
              {t.withdraw}
            </Button>
          </div>
        </form>
      ) : (
        <>
          {save.isError && <Notice tone="error">{errorMessage(save.error)}</Notice>}
          <Button size="touch" variant="outline" className="self-start" disabled={save.isPending} onClick={() => save.mutate()}>
            {t.offer}
          </Button>
        </>
      )}
    </li>
  );
}
