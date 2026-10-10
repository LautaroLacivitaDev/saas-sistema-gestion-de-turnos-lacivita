"use client";

import { useQuery } from "@tanstack/react-query";
import { createContext, useContext } from "react";

import { api } from "@/lib/api/client";
import type { Branch, Catalog, Member, Membership, Page, Professional, Role } from "@/lib/api/types";

// Datos que comparten todas las pantallas del panel de un negocio: quién soy ahí, sus sucursales, sus
// profesionales y su catálogo. Se cargan una vez al entrar y TanStack Query los mantiene en memoria.

const RANK: Record<Role, number> = { BARBER: 1, MANAGER: 2, OWNER: 3 };

/** {@code true} si el rol alcanza para lo pedido: el dueño puede todo lo del gerente, y este lo del barbero. */
export function atLeast(role: string, required: Role): boolean {
  return (RANK[role as Role] ?? 0) >= RANK[required];
}

export type PanelContext = {
  businessId: string;
  membership: Membership;
  userId: string;
  branches: Branch[];
  /** Quienes hacen al menos un servicio, con sus sucursales y precios. */
  professionals: Professional[];
  catalog: Catalog;
  /** El nombre de un profesional, o "" si ya no está. */
  professionalName: (userId: string) => string;
  can: (required: Role) => boolean;
};

const Context = createContext<PanelContext | null>(null);

export const PanelProvider = Context.Provider;

export function usePanel(): PanelContext {
  const context = useContext(Context);
  if (!context) {
    throw new Error("usePanel se usa dentro del panel de un negocio");
  }
  return context;
}

export function useMemberships() {
  return useQuery({ queryKey: ["memberships"], queryFn: () => api<Membership[]>("/api/memberships") });
}

export function useBranches(businessId: string) {
  return useQuery({
    queryKey: ["panel", businessId, "branches"],
    queryFn: () => api<Branch[]>(`/api/businesses/${businessId}/branches`),
  });
}

export function useMembers(businessId: string) {
  return useQuery({
    queryKey: ["panel", businessId, "members"],
    queryFn: async () => (await api<Page<Member>>(`/api/businesses/${businessId}/members?size=100`)).items,
  });
}

/** Clave de las consultas de un negocio, para invalidarlas juntas después de un cambio. */
export function panelKey(businessId: string, ...rest: unknown[]) {
  return ["panel", businessId, ...rest];
}

/** Profesionales del negocio (los que se pueden reservar). Es el listado público: lo ve todo el equipo. */
export function useProfessionals(slug: string | undefined) {
  return useQuery({
    queryKey: ["professionals", slug],
    queryFn: () => api<Professional[]>(`/api/public/businesses/${slug}/professionals`),
    enabled: Boolean(slug),
  });
}

/** Servicios y combos que se pueden reservar, con el precio de cada profesional. */
export function useCatalog(slug: string | undefined) {
  return useQuery({
    queryKey: ["catalog", slug],
    queryFn: () => api<Catalog>(`/api/public/businesses/${slug}/catalog`),
    enabled: Boolean(slug),
  });
}
