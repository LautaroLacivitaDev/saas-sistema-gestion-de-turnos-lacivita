"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import { useEffect, useRef } from "react";

import { Avatar } from "@/components/avatar";
import { Notice } from "@/components/notice";
import { api } from "@/lib/api/client";
import { ApiError, errorMessage } from "@/lib/api/errors";
import type { Availability, BookableItem, Hold, Slot } from "@/lib/api/types";
import type { Choice } from "@/lib/booking";
import { localDate, money } from "@/lib/format";
import { cn } from "@/lib/utils";
import { messages } from "@/messages/es-AR";

import { DayStrip } from "./day-strip";
import { SlotGrid } from "./slot-grid";

/** Horarios libres de un día para lo elegido. Sin profesional, es "cualquiera disponible". */
export function useAvailability(
  slug: string,
  branchId: string,
  item: BookableItem,
  date: string,
  barberId: string | null,
) {
  return useQuery({
    queryKey: ["availability", slug, branchId, item.kind, item.id, date, barberId],
    queryFn: () => {
      const params = new URLSearchParams({ branchId, date });
      params.set(item.kind === "service" ? "serviceId" : "comboId", item.id);
      if (barberId) {
        params.set("barberId", barberId);
      }
      return api<Availability>(`/api/public/businesses/${slug}/availability?${params}`);
    },
    staleTime: 30_000,
  });
}

/** Paso 2: profesional (o cualquiera), día y horario. Al tocar un horario, queda reservado unos minutos. */
export function TimeStep({
  slug,
  branchId,
  timeZone,
  item,
  choices,
  barberId,
  date,
  notice,
  onBarber,
  onDate,
  onHeld,
}: {
  slug: string;
  branchId: string;
  timeZone: string;
  item: BookableItem;
  choices: Choice[];
  barberId: string | null;
  date: string;
  notice: string | null;
  onBarber: (barberId: string | null) => void;
  onDate: (date: string) => void;
  onHeld: (hold: Hold) => void;
}) {
  const t = messages.booking;
  const chosenBarber = choices.some((choice) => choice.barberId === barberId) ? barberId : null;
  const availability = useAvailability(slug, branchId, item, date, chosenBarber);
  const hold = useMutation({
    mutationFn: (slot: Slot) =>
      api<Hold>(`/api/public/businesses/${slug}/holds`, {
        method: "POST",
        body: {
          branchId,
          [item.kind === "service" ? "serviceId" : "comboId"]: item.id,
          startsAt: slot.startsAt,
          ...(chosenBarber ? { barberId: chosenBarber } : {}),
        },
      }),
    onSuccess: onHeld,
    onError: (error) => {
      if (error instanceof ApiError && error.code === "slot_not_available") {
        void availability.refetch();
      }
    },
  });

  const holdError =
    hold.error instanceof ApiError && hold.error.code === "slot_not_available" ? t.taken : hold.error ? errorMessage(hold.error) : null;

  return (
    <>
      {notice && <Notice tone="error">{notice}</Notice>}

      <fieldset className="flex min-w-0 flex-col gap-2">
        <legend className="mb-2 text-sm font-medium">{t.professional}</legend>
        <div className="-mx-4 flex gap-2 overflow-x-auto px-4 pb-1 [scrollbar-width:none]">
          <ProfessionalChip
            active={chosenBarber === null}
            name={t.anyone}
            detail={t.anyoneHint}
            onClick={() => onBarber(null)}
          />
          {choices.map((choice) => (
            <ProfessionalChip
              key={choice.barberId}
              active={chosenBarber === choice.barberId}
              name={choice.barberName}
              detail={money(choice.price)}
              photoUrl={choice.photoUrl}
              onClick={() => onBarber(choice.barberId)}
            />
          ))}
        </div>
      </fieldset>

      <fieldset className="flex min-w-0 flex-col gap-2">
        <legend className="mb-2 text-sm font-medium">{t.day}</legend>
        <DayStrip today={localDate(new Date(), timeZone)} selected={date} onSelect={onDate} />
      </fieldset>

      <section className="flex flex-col gap-2" aria-labelledby="horarios">
        <h2 id="horarios" className="text-sm font-medium">
          {t.times}
        </h2>
        {holdError && <Notice tone="error">{holdError}</Notice>}
        {hold.isPending && <Notice>{t.holding}</Notice>}
        {availability.isError ? (
          <Notice tone="error">{errorMessage(availability.error)}</Notice>
        ) : (
          <SlotGrid
            slots={availability.data?.slots}
            loading={availability.isPending}
            disabled={hold.isPending}
            onSelect={(slot) => hold.mutate(slot)}
          />
        )}
      </section>
    </>
  );
}

function ProfessionalChip({
  active,
  name,
  detail,
  photoUrl = null,
  onClick,
}: {
  active: boolean;
  name: string;
  detail: string;
  photoUrl?: string | null;
  onClick: () => void;
}) {
  // Si se llegó con un profesional elegido (por ejemplo, desde su perfil), se lo muestra sin tener que
  // desplazar la lista.
  const ref = useRef<HTMLButtonElement>(null);
  useEffect(() => {
    if (active) {
      ref.current?.scrollIntoView({ block: "nearest", inline: "nearest" });
    }
  }, [active]);
  return (
    <button
      ref={ref}
      type="button"
      aria-pressed={active}
      onClick={onClick}
      className={cn(
        "flex min-h-14 shrink-0 items-center gap-2 rounded-xl border px-3 py-2 text-left transition-colors",
        active ? "border-primary ring-2 ring-primary/30" : "hover:bg-muted",
      )}
    >
      <Avatar name={name} photoUrl={photoUrl} size={36} />
      <span className="flex flex-col">
        <span className="text-sm font-medium">{name}</span>
        <span className="text-xs text-muted-foreground">{detail}</span>
      </span>
    </button>
  );
}
