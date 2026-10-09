"use client";

import { BottomBar, BottomBarSpacer } from "@/components/bottom-bar";
import { Button } from "@/components/ui/button";
import type { BookableItem, BusinessPage, Catalog, Professional } from "@/lib/api/types";
import { choicesFor } from "@/lib/booking";
import { money } from "@/lib/format";
import { cn } from "@/lib/utils";
import { messages } from "@/messages/es-AR";

type Option = { item: BookableItem; name: string; detail: string; fromPrice: number };

/** Paso 1: sucursal (si hay más de una) y el servicio o combo. */
export function ServiceStep({
  business,
  catalog,
  professionals,
  branchId,
  item,
  onBranch,
  onItem,
  onContinue,
}: {
  business: BusinessPage;
  catalog: Catalog;
  professionals: Professional[];
  branchId: string | undefined;
  item: BookableItem | undefined;
  onBranch: (branchId: string) => void;
  onItem: (item: BookableItem) => void;
  onContinue: () => void;
}) {
  const t = messages.booking;
  const options: Option[] = [
    ...catalog.services.map((service) => ({
      item: { kind: "service", id: service.id } as BookableItem,
      name: service.name,
      detail: service.category,
      fromPrice: service.fromPrice,
    })),
    ...catalog.combos.map((combo) => ({
      item: { kind: "combo", id: combo.id } as BookableItem,
      name: combo.name,
      detail: messages.business.combos,
      fromPrice: combo.fromPrice,
    })),
  ];
  // En la sucursal elegida, solo lo que hace alguien que atiende ahí.
  const available = branchId
    ? options.filter((option) => choicesFor(catalog, professionals, option.item, branchId).length > 0)
    : options;
  const chosen = available.find((option) => option.item.kind === item?.kind && option.item.id === item?.id);

  return (
    <>
      {business.branches.length > 1 && (
        <fieldset className="flex min-w-0 flex-col gap-2">
          <legend className="mb-2 text-sm font-medium">{t.branch}</legend>
          <div className="flex flex-wrap gap-2">
            {business.branches.map((branch) => (
              <button
                key={branch.id}
                type="button"
                aria-pressed={branch.id === branchId}
                onClick={() => onBranch(branch.id)}
                className={cn(
                  "flex min-h-11 flex-col items-start rounded-xl border px-4 py-2 text-left transition-colors",
                  branch.id === branchId ? "border-primary bg-primary text-primary-foreground" : "hover:bg-muted",
                )}
              >
                <span className="font-medium">{branch.name}</span>
                <span className="text-sm opacity-80">{branch.street}</span>
              </button>
            ))}
          </div>
        </fieldset>
      )}

      <fieldset className="flex min-w-0 flex-col gap-2" disabled={!branchId}>
        <legend className="mb-2 text-sm font-medium">{t.service}</legend>
        <ul className="flex flex-col gap-2">
          {available.map((option) => {
            const active = option === chosen;
            return (
              <li key={`${option.item.kind}-${option.item.id}`}>
                <button
                  type="button"
                  aria-pressed={active}
                  onClick={() => onItem(option.item)}
                  className={cn(
                    "flex min-h-14 w-full items-center justify-between gap-4 rounded-xl border px-4 py-3 text-left transition-colors disabled:opacity-50",
                    active ? "border-primary ring-2 ring-primary/30" : "hover:bg-muted",
                  )}
                >
                  <span className="flex min-w-0 flex-col">
                    <span className="font-medium">{option.name}</span>
                    <span className="text-sm text-muted-foreground">{option.detail}</span>
                  </span>
                  <span className="shrink-0 text-sm">{messages.business.from(money(option.fromPrice))}</span>
                </button>
              </li>
            );
          })}
        </ul>
      </fieldset>

      <BottomBarSpacer />
      <BottomBar>
        <Button size="touch" className="w-full" disabled={!branchId || !chosen} onClick={onContinue}>
          {t.continue}
        </Button>
      </BottomBar>
    </>
  );
}
