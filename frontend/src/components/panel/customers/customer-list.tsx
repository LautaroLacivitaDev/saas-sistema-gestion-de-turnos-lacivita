"use client";

import { useInfiniteQuery } from "@tanstack/react-query";
import { SearchIcon } from "lucide-react";
import Link from "next/link";
import { useState } from "react";

import { Notice } from "@/components/notice";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import type { CustomerSummary, Page } from "@/lib/api/types";
import { dayParts, localDate } from "@/lib/format";
import { panelKey, usePanel } from "@/lib/panel";
import { useDebounced } from "@/lib/use-debounced";
import { messages } from "@/messages/es-AR";

const t = messages.panel.customers;

/** Base de clientes: buscar mientras se escribe y entrar a la ficha de cada uno. */
export function CustomerList() {
  const panel = usePanel();
  const [text, setText] = useState("");
  const query = useDebounced(text.trim());
  const timeZone = panel.branches[0]?.timeZone ?? "America/Argentina/Buenos_Aires";

  const customers = useInfiniteQuery({
    queryKey: panelKey(panel.businessId, "customers", query),
    queryFn: ({ pageParam }) =>
      api<Page<CustomerSummary>>(
        `/api/businesses/${panel.businessId}/customers?${new URLSearchParams({ q: query, page: String(pageParam) })}`,
      ),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.page + 1 < last.totalPages ? last.page + 1 : undefined),
  });
  const items = customers.data?.pages.flatMap((page) => page.items) ?? [];

  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-2xl font-semibold tracking-tight">{t.title}</h1>
      <label className="relative">
        <span className="sr-only">{t.search}</span>
        <SearchIcon aria-hidden className="pointer-events-none absolute top-3 left-3 size-5 text-muted-foreground" />
        <input
          type="search"
          inputMode="search"
          autoComplete="off"
          value={text}
          onChange={(event) => setText(event.target.value)}
          placeholder={t.search}
          className="h-11 w-full rounded-lg border border-input bg-background pr-3 pl-10 text-base outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
        />
      </label>

      {customers.isError && <Notice tone="error">{errorMessage(customers.error)}</Notice>}
      {customers.isPending ? (
        <p className="text-muted-foreground">{messages.panel.loading}</p>
      ) : items.length === 0 ? (
        <p className="text-muted-foreground">{query ? t.empty : t.emptyAll}</p>
      ) : (
        <ul className="flex flex-col gap-2">
          {items.map((customer) => (
            <li key={customer.id}>
              <Link
                href={`/panel/${panel.businessId}/clientes/${customer.id}`}
                className="flex min-h-14 flex-col gap-0.5 rounded-xl border px-4 py-3 hover:bg-muted/50"
              >
                <span className="font-medium">{customer.name}</span>
                <span className="truncate text-sm text-muted-foreground">
                  {[customer.phone, customer.email].filter(Boolean).join(" · ")}
                </span>
                <span className="text-sm text-muted-foreground">
                  {t.appointments(customer.appointments)}
                  {customer.lastAppointmentAt && ` · ${t.last(shortDate(customer.lastAppointmentAt, timeZone))}`}
                </span>
              </Link>
            </li>
          ))}
        </ul>
      )}
      {customers.hasNextPage && (
        <Button
          variant="outline"
          size="touch"
          className="self-center"
          disabled={customers.isFetchingNextPage}
          onClick={() => customers.fetchNextPage()}
        >
          {messages.panel.more}
        </Button>
      )}
    </div>
  );
}

function shortDate(instant: string, timeZone: string): string {
  const parts = dayParts(localDate(new Date(instant), timeZone));
  return `${parts.day} ${parts.month}`;
}
