"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";

import { Field } from "@/components/field";
import { Notice } from "@/components/notice";
import { SelectField } from "@/components/select-field";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import type { BarberSchedule, Branch, ScheduleRules, Week } from "@/lib/api/types";
import { panelKey, useManagedBranches, useMembers, usePanel } from "@/lib/panel";
import { messages } from "@/messages/es-AR";

import { canManage } from "../team/team";
import { Blocks } from "./blocks";
import { HoursEditor } from "./hours-editor";

const t = messages.panel.schedules;

type Person = { userId: string; name: string; branchIds: string[] };

/**
 * Horario de cada profesional en sus sucursales y sus bloqueos. Cada uno edita el suyo; el gerente, el de
 * los profesionales de sus sucursales; el dueño, el de todos y además las reglas de la agenda.
 */
export function Schedules() {
  const panel = usePanel();
  const self: Person = { userId: panel.userId, name: t.professional, branchIds: panel.membership.branchIds };
  const people = usePeople(self);
  const [selectedId, setSelectedId] = useState(panel.userId);
  const person = people.find((candidate) => candidate.userId === selectedId) ?? self;
  // El dueño no tiene sucursales asignadas: trabaja en todas.
  const branches =
    person.branchIds.length === 0
      ? panel.branches
      : panel.branches.filter((branch) => person.branchIds.includes(branch.id));

  return (
    <div className="flex flex-col gap-6">
      <h1 className="text-2xl font-semibold tracking-tight">{t.title}</h1>
      {people.length > 1 && (
        <SelectField label={t.professional} value={person.userId} onChange={(event) => setSelectedId(event.target.value)}>
          {people.map((candidate) => (
            <option key={candidate.userId} value={candidate.userId}>
              {candidate.name}
            </option>
          ))}
        </SelectField>
      )}
      {branches.length === 0 ? (
        <p className="text-muted-foreground">{t.noBranches}</p>
      ) : (
        <WorkSchedule key={person.userId} barberId={person.userId} branches={branches} />
      )}
      {branches.length > 0 && (
        <section aria-labelledby="bloqueos" className="flex flex-col gap-3">
          <h2 id="bloqueos" className="text-lg font-semibold">
            {messages.panel.blocks.title}
          </h2>
          <Blocks key={person.userId} owner={{ kind: "barber", barberId: person.userId }} timeZone={branches[0].timeZone} />
        </section>
      )}
      {panel.can("OWNER") && <Rules />}
    </div>
  );
}

/** A quién puede editar el horario quien ingresó: a sí mismo y, si gestiona equipo, a los de sus sucursales. */
function usePeople(self: Person): Person[] {
  const panel = usePanel();
  const manager = panel.can("MANAGER");
  const members = useMembers(panel.businessId, manager);
  const managed = new Set(useManagedBranches().map((branch) => branch.id));
  if (!manager || !members.data) {
    return [self];
  }
  return members.data
    .filter((member) => member.userId === panel.userId || canManage(member, panel.can("OWNER"), managed))
    .map(({ userId, name, branchIds }) => ({ userId, name, branchIds }));
}

function WorkSchedule({ barberId, branches }: { barberId: string; branches: Branch[] }) {
  const panel = usePanel();
  const client = useQueryClient();
  const base = `/api/businesses/${panel.businessId}/barbers/${barberId}/schedule`;
  const key = panelKey(panel.businessId, "work-schedule", barberId);
  const schedule = useQuery({ queryKey: key, queryFn: () => api<BarberSchedule[]>(base) });
  if (schedule.isPending) {
    return <p className="text-muted-foreground">{messages.panel.loading}</p>;
  }
  if (schedule.isError) {
    return <Notice tone="error">{errorMessage(schedule.error)}</Notice>;
  }
  const byBranch = new Map(schedule.data.map((entry) => [entry.branchId, entry.hours]));
  return (
    <>
      {branches.map((branch) => (
        <section key={branch.id} aria-labelledby={`horario-${branch.id}`} className="flex flex-col gap-3">
          <h2 id={`horario-${branch.id}`} className="text-lg font-semibold">
            {t.myHours(branch.name)}
          </h2>
          <HoursEditor
            id={`work-${branch.id}`}
            initial={byBranch.get(branch.id) ?? { days: [] }}
            onSave={async (week: Week) => {
              await api(`${base}/${branch.id}`, { method: "PUT", body: week });
              await client.invalidateQueries({ queryKey: key });
            }}
          />
        </section>
      ))}
    </>
  );
}

/** Reglas de la agenda de todo el negocio. Solo el dueño. */
function Rules() {
  const panel = usePanel();
  const url = `/api/businesses/${panel.businessId}/schedule-rules`;
  const rules = useQuery({ queryKey: panelKey(panel.businessId, "schedule-rules"), queryFn: () => api<ScheduleRules>(url) });
  return (
    <section aria-labelledby="reglas" className="flex flex-col gap-3 border-t pt-6">
      <div className="flex flex-col gap-1">
        <h2 id="reglas" className="text-lg font-semibold">
          {t.rules}
        </h2>
        <p className="text-sm text-muted-foreground">{t.rulesHint}</p>
      </div>
      {rules.isError && <Notice tone="error">{errorMessage(rules.error)}</Notice>}
      {rules.data && <RulesForm url={url} initial={rules.data} />}
    </section>
  );
}

function RulesForm({ url, initial }: { url: string; initial: ScheduleRules }) {
  const [values, setValues] = useState(() => ({
    bufferMinutes: String(initial.bufferMinutes),
    minNoticeMinutes: String(initial.minNoticeMinutes),
    maxAdvanceDays: String(initial.maxAdvanceDays),
    slotStepMinutes: String(initial.slotStepMinutes),
  }));
  const save = useMutation({
    mutationFn: () =>
      api(url, {
        method: "PUT",
        body: Object.fromEntries(Object.entries(values).map(([field, value]) => [field, Number(value)])),
      }),
  });
  const set = (field: keyof typeof values) => (event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement>) => {
    save.reset();
    setValues((current) => ({ ...current, [field]: event.target.value }));
  };
  return (
    <form
      className="flex flex-col gap-4"
      onSubmit={(event) => {
        event.preventDefault();
        save.mutate();
      }}
    >
      <div className="grid gap-4 sm:grid-cols-2">
        <Field id="rules-buffer" label={t.buffer} type="number" inputMode="numeric" min={0} max={120} step={5} required value={values.bufferMinutes} onChange={set("bufferMinutes")} />
        <Field id="rules-notice" label={t.minNotice} type="number" inputMode="numeric" min={0} max={10080} required value={values.minNoticeMinutes} onChange={set("minNoticeMinutes")} />
        <Field id="rules-advance" label={t.maxAdvance} type="number" inputMode="numeric" min={1} max={365} required value={values.maxAdvanceDays} onChange={set("maxAdvanceDays")} />
        <SelectField label={t.slotStep} value={values.slotStepMinutes} onChange={set("slotStepMinutes")}>
          {[5, 10, 15, 20, 30, 60].map((minutes) => (
            <option key={minutes} value={minutes}>
              {minutes} min
            </option>
          ))}
        </SelectField>
      </div>
      {save.isError && <Notice tone="error">{errorMessage(save.error)}</Notice>}
      {save.isSuccess && <Notice tone="success">{messages.panel.saved}</Notice>}
      <Button type="submit" size="touch" className="self-start" disabled={save.isPending}>
        {save.isPending ? messages.panel.saving : messages.panel.save}
      </Button>
    </form>
  );
}
