import "server-only";

import { cacheLife } from "next/cache";

import { apiErrorFrom } from "./errors";
import type {
  BusinessPage,
  Catalog,
  Professional,
  SearchResults,
  WeeklyHours,
} from "./types";

// Lecturas públicas desde el servidor de Next (Server Components). Van directo al backend, sin pasar por
// el navegador. Las páginas públicas cambian poco: se guardan en caché unos minutos (cacheLife "minutes").
const backendUrl = process.env.BACKEND_URL ?? "http://localhost:8080";

/** GET al backend. Devuelve null si no existe (404), para poder mostrar "no encontrado". */
async function get<T>(path: string): Promise<T | null> {
  const response = await fetch(`${backendUrl}${path}`, { headers: { Accept: "application/json" } });
  if (response.status === 404) {
    return null;
  }
  if (!response.ok) {
    throw await apiErrorFrom(response);
  }
  return (await response.json()) as T;
}

const business = (slug: string) => `/api/public/businesses/${encodeURIComponent(slug)}`;

export async function getBusiness(slug: string): Promise<BusinessPage | null> {
  "use cache";
  cacheLife("minutes");
  return get<BusinessPage>(business(slug));
}

export async function getCatalog(slug: string): Promise<Catalog> {
  "use cache";
  cacheLife("minutes");
  return (await get<Catalog>(`${business(slug)}/catalog`)) ?? { services: [], combos: [] };
}

export async function getProfessionals(slug: string): Promise<Professional[]> {
  "use cache";
  cacheLife("minutes");
  return (await get<Professional[]>(`${business(slug)}/professionals`)) ?? [];
}

export async function getProfessional(slug: string, barberId: string): Promise<Professional | null> {
  "use cache";
  cacheLife("minutes");
  return get<Professional>(`${business(slug)}/professionals/${encodeURIComponent(barberId)}`);
}

export async function getBranchHours(slug: string, branchId: string): Promise<WeeklyHours | null> {
  "use cache";
  cacheLife("hours");
  return get<WeeklyHours>(`${business(slug)}/branches/${encodeURIComponent(branchId)}/hours`);
}

/** Resultados del buscador. Se guardan poco: aparecen negocios nuevos. */
export async function searchBusinesses(query: string, page: number): Promise<SearchResults> {
  "use cache";
  cacheLife("minutes");
  const params = new URLSearchParams({ page: String(page), size: "20" });
  if (query) {
    params.set("q", query);
  }
  return (await get<SearchResults>(`/api/public/businesses?${params}`)) ?? {
    items: [],
    page,
    hasMore: false,
  };
}
