import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";

import { SearchForm } from "@/components/search-form";
import { searchBusinesses } from "@/lib/api/server";
import { messages } from "@/messages/es-AR";

export const metadata: Metadata = { title: messages.search.title };

const t = messages.search;

function firstValue(value: string | string[] | undefined): string {
  return (Array.isArray(value) ? value[0] : value)?.trim() ?? "";
}

export default function SearchPage({ searchParams }: PageProps<"/buscar">) {
  return (
    <main className="mx-auto flex w-full max-w-3xl flex-1 flex-col gap-6 px-4 py-6">
      <h1 className="text-2xl font-semibold tracking-tight">{t.title}</h1>
      <Suspense fallback={<SearchForm />}>
        <SearchBox searchParams={searchParams} />
      </Suspense>
      <Suspense fallback={<p className="text-muted-foreground">{t.loading}</p>}>
        <Results searchParams={searchParams} />
      </Suspense>
    </main>
  );
}

async function SearchBox({ searchParams }: Pick<PageProps<"/buscar">, "searchParams">) {
  return <SearchForm defaultValue={firstValue((await searchParams).q)} />;
}

async function Results({ searchParams }: Pick<PageProps<"/buscar">, "searchParams">) {
  const params = await searchParams;
  const query = firstValue(params.q);
  const page = Math.max(Number.parseInt(firstValue(params.page), 10) || 0, 0);
  const results = await searchBusinesses(query, page);

  if (results.items.length === 0) {
    return <p className="text-muted-foreground">{query ? t.empty : t.emptyAll}</p>;
  }
  return (
    <section className="flex flex-col gap-3" aria-label={query ? t.resultsFor(query) : t.title}>
      {query && <p className="text-sm text-muted-foreground">{t.resultsFor(query)}</p>}
      <ul className="flex flex-col gap-3">
        {results.items.map((business) => (
          <li key={business.slug}>
            <Link
              href={`/${business.slug}`}
              className="flex flex-col gap-1 rounded-xl border p-4 transition-colors hover:bg-muted/50"
            >
              <span className="text-lg font-medium">{business.name}</span>
              <span className="text-sm text-muted-foreground">
                {[messages.categories[business.category], ...business.places].filter(Boolean).join(" · ")}
              </span>
              {business.description && (
                <span className="line-clamp-2 text-sm text-muted-foreground">{business.description}</span>
              )}
            </Link>
          </li>
        ))}
      </ul>
      {results.hasMore && (
        <Link
          href={`/buscar?${new URLSearchParams({ ...(query ? { q: query } : {}), page: String(page + 1) })}`}
          className="mx-auto flex h-11 items-center px-4 text-base underline underline-offset-4"
        >
          {t.more}
        </Link>
      )}
    </section>
  );
}
