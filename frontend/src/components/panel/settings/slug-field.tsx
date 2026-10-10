"use client";

import { useQuery } from "@tanstack/react-query";

import { api } from "@/lib/api/client";
import { normalizeSlug } from "@/lib/slug";
import { siteUrl } from "@/lib/site";
import { useDebounced } from "@/lib/use-debounced";
import { messages } from "@/messages/es-AR";

const t = messages.panel.settings;

type SlugCheck = { slug: string; available: boolean; code: string | null; message: string | null };

/** Si el link que se está escribiendo está libre. No consulta el que ya tiene el negocio. */
export function useSlugCheck(slug: string, current?: string) {
  const debounced = useDebounced(slug.trim(), 400);
  return useQuery({
    queryKey: ["slug-availability", debounced],
    queryFn: () => api<SlugCheck>(`/api/businesses/slug-availability?slug=${encodeURIComponent(debounced)}`),
    enabled: debounced.length > 0 && debounced !== current,
    staleTime: 30_000,
  });
}

/** Campo del link de la página pública, con el dominio adelante y el aviso de si está libre. */
export function SlugField({
  value,
  onChange,
  current,
}: {
  value: string;
  onChange: (value: string) => void;
  current?: string;
}) {
  const check = useSlugCheck(value, current);
  const unchanged = value.trim() === current;
  const status =
    unchanged || !value.trim()
      ? null
      : check.isFetching || check.isPending
        ? { tone: "text-muted-foreground", text: t.checking }
        : check.data?.available
          ? { tone: "text-emerald-700 dark:text-emerald-400", text: t.available }
          : { tone: "text-destructive", text: check.data?.message ?? "" };
  const host = siteUrl.replace(/^https?:\/\//, "");
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor="business-slug" className="text-sm font-medium">
        {t.link}
      </label>
      <div className="flex h-11 min-w-0 items-center rounded-lg border border-input bg-background focus-within:border-ring focus-within:ring-3 focus-within:ring-ring/50">
        <span className="max-w-[55%] truncate pl-3 text-base text-muted-foreground">{host}/</span>
        <input
          id="business-slug"
          autoCapitalize="none"
          autoCorrect="off"
          spellCheck={false}
          autoComplete="off"
          required
          maxLength={50}
          value={value}
          onChange={(event) => onChange(normalizeSlug(event.target.value))}
          aria-describedby="business-slug-hint"
          className="h-full min-w-0 flex-1 bg-transparent pr-3 text-base outline-none"
        />
      </div>
      <p id="business-slug-hint" className="text-sm text-muted-foreground">
        {t.linkHint}
      </p>
      {status && (
        <p role="status" className={`text-sm ${status.tone}`}>
          {status.text}
        </p>
      )}
    </div>
  );
}
