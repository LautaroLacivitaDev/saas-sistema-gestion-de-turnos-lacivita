"use client";

import type { Branch } from "@/lib/api/types";
import { messages } from "@/messages/es-AR";

const t = messages.panel.team;

/** Casillas para elegir las sucursales donde trabaja alguien. */
export function BranchPicker({
  branches,
  value,
  onChange,
}: {
  branches: Branch[];
  value: string[];
  onChange: (value: string[]) => void;
}) {
  const toggle = (id: string) => onChange(value.includes(id) ? value.filter((v) => v !== id) : [...value, id]);
  return (
    <fieldset className="flex min-w-0 flex-col gap-2">
      <legend className="mb-1 text-sm font-medium">{t.branches}</legend>
      <p className="text-sm text-muted-foreground">{t.branchesHint}</p>
      {branches.map((branch) => (
        <label key={branch.id} className="flex min-h-11 items-center gap-3 rounded-lg border px-3">
          <input type="checkbox" className="size-5" checked={value.includes(branch.id)} onChange={() => toggle(branch.id)} />
          {branch.name}
        </label>
      ))}
    </fieldset>
  );
}
