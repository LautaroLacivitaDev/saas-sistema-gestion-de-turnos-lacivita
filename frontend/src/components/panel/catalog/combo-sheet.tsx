"use client";

import { useMutation } from "@tanstack/react-query";
import { useState } from "react";

import { Field } from "@/components/field";
import { Notice } from "@/components/notice";
import { Button } from "@/components/ui/button";
import { Sheet } from "@/components/ui/sheet";
import { api } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import type { Combo, TeamService } from "@/lib/api/types";
import { money } from "@/lib/format";
import { usePanel } from "@/lib/panel";
import { messages } from "@/messages/es-AR";

import { useRefreshCatalog } from "./catalog";

const t = messages.panel.catalog;

/** Crear o editar un combo: un nombre y de 2 a 5 servicios activos. */
export function ComboSheet({
  combo,
  services,
  onClose,
}: {
  combo: Combo | "new" | null;
  services: TeamService[];
  onClose: () => void;
}) {
  return (
    <Sheet open={combo !== null} onOpenChange={(open) => !open && onClose()} title={combo === "new" ? t.newCombo : t.editCombo}>
      {combo !== null && (
        <ComboEditor
          key={combo === "new" ? "new" : combo.id}
          combo={combo === "new" ? null : combo}
          services={services.filter((service) => service.status === "ACTIVE")}
          onDone={onClose}
        />
      )}
    </Sheet>
  );
}

function ComboEditor({ combo, services, onDone }: { combo: Combo | null; services: TeamService[]; onDone: () => void }) {
  const panel = usePanel();
  const refresh = useRefreshCatalog();
  const [name, setName] = useState(combo?.name ?? "");
  const [chosen, setChosen] = useState<string[]>(combo?.serviceIds ?? []);
  const [active, setActive] = useState(combo?.active ?? true);
  const save = useMutation({
    mutationFn: () => {
      const base = `/api/businesses/${panel.businessId}/combos`;
      return combo
        ? api(`${base}/${combo.id}`, { method: "PUT", body: { name, serviceIds: chosen, active } })
        : api(base, { method: "POST", body: { name, serviceIds: chosen } });
    },
    onSuccess: () => {
      refresh();
      onDone();
    },
  });
  const toggle = (id: string) =>
    setChosen((current) => (current.includes(id) ? current.filter((value) => value !== id) : [...current, id]));

  return (
    <form
      className="flex flex-col gap-4"
      onSubmit={(event) => {
        event.preventDefault();
        save.mutate();
      }}
    >
      <Field id="combo-name" label={t.name} autoComplete="off" value={name} onChange={(e) => setName(e.target.value)} required />
      <fieldset className="flex min-w-0 flex-col gap-2">
        <legend className="mb-1 text-sm font-medium">{t.comboServices}</legend>
        <p className="text-sm text-muted-foreground">{t.comboHint}</p>
        {services.map((service) => (
          <label key={service.id} className="flex min-h-11 items-center gap-3 rounded-lg border px-3">
            <input
              type="checkbox"
              className="size-5"
              checked={chosen.includes(service.id)}
              onChange={() => toggle(service.id)}
            />
            <span className="flex-1">{service.name}</span>
            <span className="text-sm text-muted-foreground">{money(service.basePrice)}</span>
          </label>
        ))}
      </fieldset>
      {combo && (
        <label className="flex min-h-11 items-center gap-3">
          <input type="checkbox" className="size-5" checked={active} onChange={(e) => setActive(e.target.checked)} />
          {t.active}
        </label>
      )}
      {save.isError && <Notice tone="error">{errorMessage(save.error)}</Notice>}
      <Button type="submit" size="touch" disabled={save.isPending || chosen.length < 2 || !name.trim()}>
        {save.isPending ? messages.panel.saving : messages.panel.save}
      </Button>
    </form>
  );
}
