"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { PlusIcon } from "lucide-react";
import { useState } from "react";

import { Field } from "@/components/field";
import { Notice } from "@/components/notice";
import { Button } from "@/components/ui/button";
import { Sheet } from "@/components/ui/sheet";
import { api } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import type { Page, TimeBlock } from "@/lib/api/types";
import { localDate, shortDateTime, zonedInstant } from "@/lib/format";
import { panelKey, usePanel } from "@/lib/panel";
import { useNow } from "@/lib/use-now";
import { messages } from "@/messages/es-AR";

const t = messages.panel.blocks;
const WINDOW_DAYS = 90;

/** De quién son los bloqueos: una sucursal entera o un profesional. */
export type BlockOwner = { kind: "branch"; branchId: string } | { kind: "barber"; barberId: string };

/**
 * Bloqueos de los próximos 3 meses, con alta y baja. La API devuelve los de todo el negocio en el período;
 * acá se muestran solo los de la sucursal o del profesional.
 */
export function Blocks({ owner, timeZone }: { owner: BlockOwner; timeZone: string }) {
  const panel = usePanel();
  const client = useQueryClient();
  const now = useNow();
  const [adding, setAdding] = useState(false);
  const key = panelKey(panel.businessId, "time-blocks");
  const blocks = useQuery({
    queryKey: key,
    queryFn: async () => {
      const from = new Date(now).toISOString();
      const to = new Date(now + WINDOW_DAYS * 86_400_000).toISOString();
      return (
        await api<Page<TimeBlock>>(
          `/api/businesses/${panel.businessId}/time-blocks?from=${from}&to=${to}&size=100`,
        )
      ).items;
    },
  });
  const remove = useMutation({
    mutationFn: (id: string) => api(`/api/businesses/${panel.businessId}/time-blocks/${id}`, { method: "DELETE" }),
    onSuccess: () => client.invalidateQueries({ queryKey: key }),
  });
  const mine = (blocks.data ?? []).filter((block) =>
    owner.kind === "branch"
      ? block.barberId === null && block.branchId === owner.branchId
      : block.barberId === owner.barberId,
  );

  return (
    <div className="flex flex-col gap-3">
      {(blocks.isError || remove.isError) && <Notice tone="error">{errorMessage(blocks.error ?? remove.error)}</Notice>}
      {blocks.isPending ? (
        <p className="text-muted-foreground">{messages.panel.loading}</p>
      ) : mine.length === 0 ? (
        <p className="text-muted-foreground">{t.empty}</p>
      ) : (
        <ul className="flex flex-col gap-2">
          {mine.map((block) => (
            <li key={block.id} className="flex items-center gap-3 rounded-xl border px-4 py-3">
              <span className="flex min-w-0 flex-1 flex-col">
                <span className="text-sm font-medium">
                  {t.range(shortDateTime(block.startsAt, timeZone), shortDateTime(block.endsAt, timeZone))}
                </span>
                {block.reason && <span className="text-sm text-muted-foreground">{block.reason}</span>}
              </span>
              <Button
                size="touch"
                variant="outline"
                className="shrink-0"
                disabled={remove.isPending}
                onClick={() => remove.mutate(block.id)}
              >
                {t.remove}
              </Button>
            </li>
          ))}
        </ul>
      )}
      <Button size="touch" variant="outline" className="self-start" onClick={() => setAdding(true)}>
        <PlusIcon aria-hidden />
        {t.add}
      </Button>
      <Sheet open={adding} onOpenChange={setAdding} title={t.add}>
        {adding && (
          <BlockForm
            owner={owner}
            timeZone={timeZone}
            today={localDate(new Date(now), timeZone)}
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

function BlockForm({
  owner,
  timeZone,
  today,
  onDone,
}: {
  owner: BlockOwner;
  timeZone: string;
  today: string;
  onDone: () => void;
}) {
  const panel = usePanel();
  const [date, setDate] = useState(today);
  const [endDate, setEndDate] = useState(today);
  const [from, setFrom] = useState("09:00");
  const [to, setTo] = useState("18:00");
  const [reason, setReason] = useState("");
  const save = useMutation({
    mutationFn: () => {
      const base = `/api/businesses/${panel.businessId}`;
      const url =
        owner.kind === "branch" ? `${base}/branches/${owner.branchId}/time-blocks` : `${base}/barbers/${owner.barberId}/time-blocks`;
      return api(url, {
        method: "POST",
        body: {
          startsAt: zonedInstant(date, from, timeZone),
          endsAt: zonedInstant(endDate, to, timeZone),
          reason: reason.trim() || undefined,
        },
      });
    },
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
      <p className="text-sm text-muted-foreground">{t.hint}</p>
      <div className="grid grid-cols-2 gap-3">
        <Field
          id="block-date"
          label={t.date}
          type="date"
          min={today}
          required
          value={date}
          onChange={(event) => {
            setDate(event.target.value);
            if (endDate < event.target.value) {
              setEndDate(event.target.value);
            }
          }}
        />
        <Field id="block-from" label={t.from} type="time" step={300} required value={from} onChange={(e) => setFrom(e.target.value)} />
        <Field
          id="block-end-date"
          label={t.endDate}
          type="date"
          min={date}
          required
          value={endDate}
          onChange={(event) => setEndDate(event.target.value)}
        />
        <Field id="block-to" label={t.to} type="time" step={300} required value={to} onChange={(e) => setTo(e.target.value)} />
      </div>
      <Field id="block-reason" label={t.reason} maxLength={120} autoComplete="off" value={reason} onChange={(e) => setReason(e.target.value)} />
      {save.isError && <Notice tone="error">{errorMessage(save.error)}</Notice>}
      <Button type="submit" size="touch" disabled={save.isPending}>
        {save.isPending ? messages.panel.saving : t.add}
      </Button>
    </form>
  );
}
