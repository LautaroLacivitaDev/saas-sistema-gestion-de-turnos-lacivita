"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ChevronLeftIcon, PlusIcon } from "lucide-react";
import Link from "next/link";
import { useState } from "react";

import { Field } from "@/components/field";
import { Notice } from "@/components/notice";
import { Button } from "@/components/ui/button";
import { Sheet } from "@/components/ui/sheet";
import { api } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import type { Branch, Holiday, Week } from "@/lib/api/types";
import { addDays, localDate, longDate } from "@/lib/format";
import { panelKey, useManagedBranches, usePanel } from "@/lib/panel";
import { useNow } from "@/lib/use-now";
import { messages } from "@/messages/es-AR";

import { Blocks } from "../schedule/blocks";
import { HoursEditor } from "../schedule/hours-editor";
import { BranchSheet } from "./branch-sheet";

const t = messages.panel.branches;

/** Una sucursal: datos (el dueño), horario de atención, días cerrados y cierres por horas. */
export function BranchDetail({ branchId }: { branchId: string }) {
  const panel = usePanel();
  const branch = useManagedBranches().find((candidate) => candidate.id === branchId);
  const [editing, setEditing] = useState(false);
  const back = (
    <Link
      href={`/panel/${panel.businessId}/sucursales`}
      className="flex min-h-11 items-center gap-1 self-start text-sm text-muted-foreground hover:text-foreground"
    >
      <ChevronLeftIcon aria-hidden className="size-5" />
      {t.back}
    </Link>
  );
  if (!branch) {
    return (
      <div className="flex flex-col gap-4">
        {back}
        <Notice tone="error">{t.notFound}</Notice>
      </div>
    );
  }
  return (
    <div className="flex flex-col gap-6">
      {back}
      <div className="flex items-start justify-between gap-3">
        <div className="flex min-w-0 flex-col">
          <h1 className="text-2xl font-semibold tracking-tight">{branch.name}</h1>
          <p className="text-sm text-muted-foreground">
            {[branch.street, branch.neighborhood, branch.city].filter(Boolean).join(", ")}
            {branch.phone && ` · ${branch.phone}`}
          </p>
        </div>
        {panel.can("OWNER") && (
          <Button size="touch" variant="outline" className="shrink-0" onClick={() => setEditing(true)}>
            {messages.panel.edit}
          </Button>
        )}
      </div>
      <Section id="horario" title={t.hours} hint={t.hoursHint}>
        <BranchHours branch={branch} />
      </Section>
      <Section id="feriados" title={t.holidays} hint={t.holidaysHint}>
        <Holidays branch={branch} />
      </Section>
      <Section id="cierres" title={t.closures} hint={t.closuresHint}>
        <Blocks owner={{ kind: "branch", branchId: branch.id }} timeZone={branch.timeZone} />
      </Section>
      <BranchSheet branch={editing ? branch : null} onClose={() => setEditing(false)} />
    </div>
  );
}

function Section({ id, title, hint, children }: { id: string; title: string; hint: string; children: React.ReactNode }) {
  return (
    <section aria-labelledby={id} className="flex flex-col gap-3">
      <div className="flex flex-col gap-1">
        <h2 id={id} className="text-lg font-semibold">
          {title}
        </h2>
        <p className="text-sm text-muted-foreground">{hint}</p>
      </div>
      {children}
    </section>
  );
}

function BranchHours({ branch }: { branch: Branch }) {
  const panel = usePanel();
  const client = useQueryClient();
  const url = `/api/businesses/${panel.businessId}/branches/${branch.id}/hours`;
  const key = panelKey(panel.businessId, "branch-hours", branch.id);
  const hours = useQuery({ queryKey: key, queryFn: () => api<Week>(url) });
  if (hours.isPending) {
    return <p className="text-muted-foreground">{messages.panel.loading}</p>;
  }
  if (hours.isError) {
    return <Notice tone="error">{errorMessage(hours.error)}</Notice>;
  }
  return (
    <HoursEditor
      id={`branch-${branch.id}`}
      initial={hours.data}
      onSave={async (week) => {
        const saved = await api<Week>(url, { method: "PUT", body: week });
        client.setQueryData(key, saved);
      }}
    />
  );
}

/** Días cerrados del próximo año: los de la sucursal y los de todo el negocio. */
function Holidays({ branch }: { branch: Branch }) {
  const panel = usePanel();
  const client = useQueryClient();
  const now = useNow();
  const today = localDate(new Date(now), branch.timeZone);
  const [adding, setAdding] = useState(false);
  const key = panelKey(panel.businessId, "holidays");
  const holidays = useQuery({
    queryKey: key,
    queryFn: () =>
      api<Holiday[]>(`/api/businesses/${panel.businessId}/holidays?from=${today}&to=${addDays(today, 365)}`),
  });
  const remove = useMutation({
    mutationFn: (id: string) => api(`/api/businesses/${panel.businessId}/holidays/${id}`, { method: "DELETE" }),
    onSuccess: () => client.invalidateQueries({ queryKey: key }),
  });
  const visible = (holidays.data ?? []).filter((holiday) => holiday.branchId === null || holiday.branchId === branch.id);

  return (
    <div className="flex flex-col gap-3">
      {(holidays.isError || remove.isError) && (
        <Notice tone="error">{errorMessage(holidays.error ?? remove.error)}</Notice>
      )}
      {holidays.isPending ? (
        <p className="text-muted-foreground">{messages.panel.loading}</p>
      ) : visible.length === 0 ? (
        <p className="text-muted-foreground">{t.noHolidays}</p>
      ) : (
        <ul className="flex flex-col gap-2">
          {visible.map((holiday) => {
            // Los de todo el negocio los borra solo el dueño.
            const removable = holiday.branchId !== null || panel.can("OWNER");
            return (
              <li key={holiday.id} className="flex items-center gap-3 rounded-xl border px-4 py-3">
                <span className="flex min-w-0 flex-1 flex-col">
                  <span className="font-medium first-letter:uppercase">{longDate(holiday.date)}</span>
                  <span className="text-sm text-muted-foreground">
                    {holiday.name}
                    {holiday.branchId === null && ` · ${t.wholeBusiness}`}
                  </span>
                </span>
                {removable && (
                  <Button
                    size="touch"
                    variant="outline"
                    className="shrink-0"
                    disabled={remove.isPending}
                    onClick={() => remove.mutate(holiday.id)}
                  >
                    {messages.panel.blocks.remove}
                  </Button>
                )}
              </li>
            );
          })}
        </ul>
      )}
      <Button size="touch" variant="outline" className="self-start" onClick={() => setAdding(true)}>
        <PlusIcon aria-hidden />
        {t.addHoliday}
      </Button>
      <Sheet open={adding} onOpenChange={setAdding} title={t.addHoliday}>
        {adding && (
          <HolidayForm
            branch={branch}
            today={today}
            onDone={() => {
              setAdding(false);
              void client.invalidateQueries({ queryKey: key });
            }}
          />
        )}
      </Sheet>
    </div>
  );
}

function HolidayForm({ branch, today, onDone }: { branch: Branch; today: string; onDone: () => void }) {
  const panel = usePanel();
  const [date, setDate] = useState(today);
  const [name, setName] = useState("");
  const [wholeBusiness, setWholeBusiness] = useState(false);
  const save = useMutation({
    mutationFn: () =>
      api(`/api/businesses/${panel.businessId}/holidays`, {
        method: "POST",
        body: { date, name, branchId: wholeBusiness ? undefined : branch.id },
      }),
    onSuccess: onDone,
  });
  return (
    <form
      className="flex flex-col gap-4"
      onSubmit={(event) => {
        event.preventDefault();
        save.mutate();
      }}
    >
      <Field id="holiday-date" label={t.holidayDate} type="date" min={today} required value={date} onChange={(e) => setDate(e.target.value)} />
      <Field id="holiday-name" label={t.holidayName} autoComplete="off" required maxLength={80} value={name} onChange={(e) => setName(e.target.value)} />
      {panel.can("OWNER") && (
        <label className="flex min-h-11 items-center gap-3">
          <input type="checkbox" className="size-5" checked={wholeBusiness} onChange={(e) => setWholeBusiness(e.target.checked)} />
          {t.wholeBusiness}
        </label>
      )}
      {save.isError && <Notice tone="error">{errorMessage(save.error)}</Notice>}
      <Button type="submit" size="touch" disabled={save.isPending}>
        {save.isPending ? messages.panel.saving : t.addHoliday}
      </Button>
    </form>
  );
}
