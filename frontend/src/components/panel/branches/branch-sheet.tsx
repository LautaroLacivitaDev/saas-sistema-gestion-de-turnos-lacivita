"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";

import { Field } from "@/components/field";
import { Notice } from "@/components/notice";
import { Button } from "@/components/ui/button";
import { Sheet } from "@/components/ui/sheet";
import { api } from "@/lib/api/client";
import { ApiError, errorMessage } from "@/lib/api/errors";
import type { Branch } from "@/lib/api/types";
import { panelKey, usePanel } from "@/lib/panel";
import { messages } from "@/messages/es-AR";

const t = messages.panel.branches;

/** Alta o edición de los datos de una sucursal. Solo el dueño. */
export function BranchSheet({ branch, onClose }: { branch: Branch | "new" | null; onClose: () => void }) {
  return (
    <Sheet open={branch !== null} onOpenChange={(open) => !open && onClose()} title={branch === "new" ? t.newBranch : t.data}>
      {branch !== null && (
        <BranchForm key={branch === "new" ? "new" : branch.id} branch={branch === "new" ? null : branch} onDone={onClose} />
      )}
    </Sheet>
  );
}

function BranchForm({ branch, onDone }: { branch: Branch | null; onDone: () => void }) {
  const panel = usePanel();
  const client = useQueryClient();
  const [values, setValues] = useState({
    name: branch?.name ?? "",
    street: branch?.street ?? "",
    neighborhood: branch?.neighborhood ?? "",
    city: branch?.city ?? "",
    phone: branch?.phone ?? "",
  });
  const set = (field: keyof typeof values) => (event: React.ChangeEvent<HTMLInputElement>) =>
    setValues((current) => ({ ...current, [field]: event.target.value }));
  const save = useMutation({
    mutationFn: () => {
      const base = `/api/businesses/${panel.businessId}/branches`;
      // DECISIÓN: las coordenadas no se piden en el panel: el link al mapa de la página pública se arma con la
      // dirección. Si la sucursal ya tenía coordenadas, se conservan.
      const body = {
        ...values,
        neighborhood: values.neighborhood.trim() || undefined,
        phone: values.phone.trim() || undefined,
        latitude: branch?.latitude ?? undefined,
        longitude: branch?.longitude ?? undefined,
        timeZone: branch?.timeZone,
      };
      return branch ? api(`${base}/${branch.id}`, { method: "PUT", body }) : api(base, { method: "POST", body });
    },
    onSuccess: () => {
      void client.invalidateQueries({ queryKey: panelKey(panel.businessId, "branches") });
      onDone();
    },
  });
  const fieldErrors = save.error instanceof ApiError ? save.error.fieldErrors : {};

  return (
    <form
      className="flex flex-col gap-4"
      onSubmit={(event) => {
        event.preventDefault();
        save.mutate();
      }}
    >
      <Field id="branch-name" label={t.name} autoComplete="off" required maxLength={80} value={values.name} error={fieldErrors.name} onChange={set("name")} />
      <Field
        id="branch-street"
        label={t.street}
        hint={t.streetHint}
        autoComplete="street-address"
        required
        maxLength={150}
        value={values.street}
        error={fieldErrors.street}
        onChange={set("street")}
      />
      <Field id="branch-neighborhood" label={t.neighborhood} autoComplete="off" maxLength={80} value={values.neighborhood} onChange={set("neighborhood")} />
      <Field id="branch-city" label={t.city} autoComplete="address-level2" required maxLength={80} value={values.city} error={fieldErrors.city} onChange={set("city")} />
      <Field
        id="branch-phone"
        label={t.phone}
        type="tel"
        inputMode="tel"
        autoComplete="tel"
        maxLength={30}
        value={values.phone}
        error={fieldErrors.phone}
        onChange={set("phone")}
      />
      {save.isError && Object.keys(fieldErrors).length === 0 && <Notice tone="error">{errorMessage(save.error)}</Notice>}
      <Button type="submit" size="touch" disabled={save.isPending}>
        {save.isPending ? messages.panel.saving : messages.panel.save}
      </Button>
    </form>
  );
}
