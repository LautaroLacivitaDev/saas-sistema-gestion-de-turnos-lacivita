"use client";

import { useInfiniteQuery, useMutation, useQueryClient } from "@tanstack/react-query";

import { Notice as Message } from "@/components/notice";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import type { Notice, Page } from "@/lib/api/types";
import { shortDateTime } from "@/lib/format";
import { panelKey, usePanel } from "@/lib/panel";
import { cn } from "@/lib/utils";
import { messages } from "@/messages/es-AR";

const t = messages.panel.notices;
const PAGE_SIZE = 20;

/** Avisos de la persona en este negocio (turnos nuevos, cancelados, movidos), del más nuevo al más viejo. */
export function Notices() {
  const panel = usePanel();
  const client = useQueryClient();
  const base = `/api/businesses/${panel.businessId}/notices`;
  const key = panelKey(panel.businessId, "notices");
  const timeZone = panel.branches[0]?.timeZone ?? "America/Argentina/Buenos_Aires";
  const notices = useInfiniteQuery({
    queryKey: key,
    queryFn: ({ pageParam }) => api<Page<Notice>>(`${base}?page=${pageParam}&size=${PAGE_SIZE}`),
    initialPageParam: 0,
    getNextPageParam: (last) => (last.page + 1 < last.totalPages ? last.page + 1 : undefined),
  });
  const refresh = () => {
    void client.invalidateQueries({ queryKey: key });
    void client.invalidateQueries({ queryKey: panelKey(panel.businessId, "unread") });
  };
  const markRead = useMutation({
    mutationFn: (id: string) => api(`${base}/${id}/read`, { method: "POST" }),
    onSuccess: refresh,
  });
  const markAll = useMutation({ mutationFn: () => api(`${base}/read-all`, { method: "POST" }), onSuccess: refresh });
  const items = notices.data?.pages.flatMap((page) => page.items) ?? [];
  const anyUnread = items.some((notice) => !notice.read);

  return (
    <div className="flex flex-col gap-4">
      <div className="flex items-center justify-between gap-3">
        <h1 className="text-2xl font-semibold tracking-tight">{t.title}</h1>
        {anyUnread && (
          <Button size="touch" variant="outline" disabled={markAll.isPending} onClick={() => markAll.mutate()}>
            {t.markAll}
          </Button>
        )}
      </div>
      {(notices.isError || markRead.isError || markAll.isError) && (
        <Message tone="error">{errorMessage(notices.error ?? markRead.error ?? markAll.error)}</Message>
      )}
      {notices.isPending ? (
        <p className="text-muted-foreground">{messages.panel.loading}</p>
      ) : items.length === 0 ? (
        <p className="text-muted-foreground">{t.empty}</p>
      ) : (
        <ul className="flex flex-col gap-2">
          {items.map((notice) => (
            <li key={notice.id}>
              <button
                type="button"
                disabled={notice.read}
                onClick={() => markRead.mutate(notice.id)}
                aria-label={notice.read ? undefined : `${notice.message}. ${t.markRead}`}
                className={cn(
                  "flex min-h-14 w-full items-start gap-3 rounded-xl border px-4 py-3 text-left",
                  !notice.read && "bg-muted/40 hover:bg-muted/70",
                )}
              >
                <span
                  aria-hidden
                  className={cn("mt-2 size-2 shrink-0 rounded-full", notice.read ? "bg-transparent" : "bg-destructive")}
                />
                <span className="flex min-w-0 flex-col">
                  <span className={cn(!notice.read && "font-medium")}>{notice.message}</span>
                  <span className="text-sm text-muted-foreground">{shortDateTime(notice.createdAt, timeZone)}</span>
                </span>
              </button>
            </li>
          ))}
        </ul>
      )}
      {notices.hasNextPage && (
        <Button
          size="touch"
          variant="outline"
          className="self-center"
          disabled={notices.isFetchingNextPage}
          onClick={() => void notices.fetchNextPage()}
        >
          {messages.panel.more}
        </Button>
      )}
    </div>
  );
}
