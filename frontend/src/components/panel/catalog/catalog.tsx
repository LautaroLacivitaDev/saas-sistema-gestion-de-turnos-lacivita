"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { PlusIcon } from "lucide-react";
import { useState } from "react";

import { Notice } from "@/components/notice";
import { Segmented } from "@/components/segmented";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import type { Combo, Page, PriceRequest, TeamService } from "@/lib/api/types";
import { money } from "@/lib/format";
import { panelKey, usePanel } from "@/lib/panel";
import { cn } from "@/lib/utils";
import { messages } from "@/messages/es-AR";

import { ComboSheet } from "./combo-sheet";
import { ServiceSheet } from "./service-sheet";

const t = messages.panel.catalog;

type Tab = "services" | "combos" | "pending";

/** Datos del catálogo del negocio que usan sus pantallas. */
export function useTeamServices() {
  const panel = usePanel();
  return useQuery({
    queryKey: panelKey(panel.businessId, "services"),
    queryFn: async () =>
      (
        await api<Page<TeamService>>(
          `/api/businesses/${panel.businessId}/services?status=ACTIVE&status=INACTIVE&status=PROPOSED&size=100`,
        )
      ).items,
  });
}

/** Después de un cambio, se vuelven a leer el catálogo del panel y el público (que usa la agenda). */
export function useRefreshCatalog() {
  const panel = usePanel();
  const client = useQueryClient();
  return () => {
    void client.invalidateQueries({ queryKey: panelKey(panel.businessId, "services") });
    void client.invalidateQueries({ queryKey: panelKey(panel.businessId, "combos") });
    void client.invalidateQueries({ queryKey: panelKey(panel.businessId, "price-requests") });
    void client.invalidateQueries({ queryKey: ["catalog", panel.membership.slug] });
    void client.invalidateQueries({ queryKey: ["professionals", panel.membership.slug] });
  };
}

/** Servicios y combos del negocio, y lo que espera aprobación (servicios propuestos y precios fuera de rango). */
export function Catalog() {
  const panel = usePanel();
  const manager = panel.can("MANAGER");
  const [tab, setTab] = useState<Tab>("services");
  const tabs: { value: Tab; label: string }[] = [
    { value: "services", label: t.services },
    { value: "combos", label: t.combos },
    ...(manager ? [{ value: "pending" as const, label: t.pending }] : []),
  ];
  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-2xl font-semibold tracking-tight">{t.title}</h1>
      <Segmented label={t.title} value={tab} options={tabs} onChange={setTab} />
      {tab === "services" && <Services />}
      {tab === "combos" && <Combos />}
      {tab === "pending" && <Pending />}
    </div>
  );
}

function Services() {
  const panel = usePanel();
  const services = useTeamServices();
  const [editing, setEditing] = useState<TeamService | "new" | null>(null);
  const visible = (services.data ?? []).filter((service) => service.status !== "PROPOSED" || panel.can("MANAGER"));
  return (
    <>
      <Button size="touch" className="self-start" onClick={() => setEditing("new")}>
        <PlusIcon aria-hidden />
        {panel.can("MANAGER") ? t.newService : t.proposeService}
      </Button>
      {services.isError && <Notice tone="error">{errorMessage(services.error)}</Notice>}
      {services.isPending ? (
        <p className="text-muted-foreground">{messages.panel.loading}</p>
      ) : visible.length === 0 ? (
        <p className="text-muted-foreground">{t.noServices}</p>
      ) : (
        <ul className="flex flex-col gap-2">
          {visible.map((service) => (
            <li key={service.id}>
              <button
                type="button"
                onClick={() => setEditing(service)}
                disabled={!panel.can("MANAGER")}
                className="flex min-h-14 w-full items-start justify-between gap-3 rounded-xl border px-4 py-3 text-left enabled:hover:bg-muted/50"
              >
                <span className="flex min-w-0 flex-col">
                  <span className="font-medium">{service.name}</span>
                  <span className="text-sm text-muted-foreground">
                    {service.category} · {service.baseDurationMinutes} min
                    {service.priceMin !== null &&
                      service.priceMax !== null &&
                      ` · ${money(service.priceMin)}–${money(service.priceMax)}`}
                  </span>
                </span>
                <span className="flex shrink-0 flex-col items-end gap-1">
                  <span className="text-sm font-medium">{money(service.basePrice)}</span>
                  <StatusBadge status={service.status} />
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}
      <ServiceSheet service={editing} onClose={() => setEditing(null)} />
    </>
  );
}

function Combos() {
  const panel = usePanel();
  const services = useTeamServices();
  const combos = useQuery({
    queryKey: panelKey(panel.businessId, "combos"),
    queryFn: async () => (await api<Page<Combo>>(`/api/businesses/${panel.businessId}/combos?size=100`)).items,
  });
  const [editing, setEditing] = useState<Combo | "new" | null>(null);
  const names = new Map((services.data ?? []).map((service) => [service.id, service.name]));
  return (
    <>
      {panel.can("MANAGER") && (
        <Button size="touch" className="self-start" onClick={() => setEditing("new")}>
          <PlusIcon aria-hidden />
          {t.newCombo}
        </Button>
      )}
      {combos.isError && <Notice tone="error">{errorMessage(combos.error)}</Notice>}
      {combos.isPending ? (
        <p className="text-muted-foreground">{messages.panel.loading}</p>
      ) : (combos.data ?? []).length === 0 ? (
        <p className="text-muted-foreground">{t.noCombos}</p>
      ) : (
        <ul className="flex flex-col gap-2">
          {(combos.data ?? []).map((combo) => (
            <li key={combo.id}>
              <button
                type="button"
                onClick={() => setEditing(combo)}
                disabled={!panel.can("MANAGER")}
                className="flex min-h-14 w-full items-start justify-between gap-3 rounded-xl border px-4 py-3 text-left enabled:hover:bg-muted/50"
              >
                <span className="flex min-w-0 flex-col">
                  <span className="font-medium">{combo.name}</span>
                  <span className="text-sm text-muted-foreground">
                    {combo.serviceIds.map((id) => names.get(id) ?? "").join(" + ")}
                  </span>
                </span>
                <StatusBadge status={combo.active ? "ACTIVE" : "INACTIVE"} />
              </button>
            </li>
          ))}
        </ul>
      )}
      <ComboSheet combo={editing} services={services.data ?? []} onClose={() => setEditing(null)} />
    </>
  );
}

function Pending() {
  const panel = usePanel();
  const services = useTeamServices();
  const refresh = useRefreshCatalog();
  const requests = useQuery({
    queryKey: panelKey(panel.businessId, "price-requests"),
    queryFn: async () =>
      (await api<Page<PriceRequest>>(`/api/businesses/${panel.businessId}/price-requests?size=100`)).items,
  });
  const decide = useMutation({
    mutationFn: (path: string) => api(`/api/businesses/${panel.businessId}/${path}`, { method: "POST" }),
    onSuccess: refresh,
  });
  const proposed = (services.data ?? []).filter((service) => service.status === "PROPOSED");
  const loading = services.isPending || requests.isPending;
  if (loading) {
    return <p className="text-muted-foreground">{messages.panel.loading}</p>;
  }
  if (proposed.length === 0 && (requests.data ?? []).length === 0) {
    return <p className="text-muted-foreground">{t.noPending}</p>;
  }
  return (
    <>
      {decide.isError && <Notice tone="error">{errorMessage(decide.error)}</Notice>}
      <ul className="flex flex-col gap-2">
      {proposed.map((service) => (
        <PendingItem
          key={service.id}
          title={`${t.proposedService}: ${service.name}`}
          detail={`${service.category} · ${service.baseDurationMinutes} min · ${money(service.basePrice)}`}
          busy={decide.isPending}
          onApprove={() => decide.mutate(`services/${service.id}/approve`)}
          onReject={() => decide.mutate(`services/${service.id}/reject`)}
        />
      ))}
      {(requests.data ?? []).map((request) => (
        <PendingItem
          key={request.offeringId}
          title={t.priceRequest(request.barberName, request.serviceName)}
          detail={t.priceChange(money(request.currentPrice), money(request.requestedPrice))}
          busy={decide.isPending}
          onApprove={() => decide.mutate(`price-requests/${request.offeringId}/approve`)}
          onReject={() => decide.mutate(`price-requests/${request.offeringId}/reject`)}
        />
      ))}
      </ul>
    </>
  );
}

function PendingItem({
  title,
  detail,
  busy,
  onApprove,
  onReject,
}: {
  title: string;
  detail: string;
  busy: boolean;
  onApprove: () => void;
  onReject: () => void;
}) {
  return (
    <li className="flex flex-col gap-3 rounded-xl border p-4">
      <div className="flex flex-col">
        <span className="font-medium">{title}</span>
        <span className="text-sm text-muted-foreground">{detail}</span>
      </div>
      <div className="flex gap-2">
        <Button size="touch" disabled={busy} onClick={onApprove}>
          {t.approve}
        </Button>
        <Button size="touch" variant="outline" disabled={busy} onClick={onReject}>
          {t.reject}
        </Button>
      </div>
    </li>
  );
}

function StatusBadge({ status }: { status: string }) {
  return (
    <span
      className={cn(
        "rounded-full px-2 py-0.5 text-xs",
        status === "ACTIVE" && "bg-emerald-50 text-emerald-800 dark:bg-emerald-950 dark:text-emerald-200",
        status === "INACTIVE" && "bg-muted text-muted-foreground",
        status === "PROPOSED" && "bg-amber-50 text-amber-800 dark:bg-amber-950 dark:text-amber-200",
      )}
    >
      {t.status[status] ?? status}
    </span>
  );
}
