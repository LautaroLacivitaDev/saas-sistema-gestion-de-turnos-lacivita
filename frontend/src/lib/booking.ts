import type {
  AccountAppointment,
  BarberTerms,
  BookableItem,
  Catalog,
  Professional,
} from "@/lib/api/types";

// Reglas del flujo de reserva que no dependen de la pantalla: qué se eligió, quién lo hace y cómo se arma
// el link para volver a reservar.

/** Lo elegido para reservar. Lo que falta, se elige en el flujo. */
export type Selection = { branchId?: string; item?: BookableItem; barberId?: string };

/** Link al flujo de reserva con lo ya elegido: "repetir", "reservar con este profesional". */
export function bookingHref(slug: string, selection: Selection = {}): string {
  const params = new URLSearchParams();
  if (selection.branchId) params.set("sucursal", selection.branchId);
  if (selection.item) params.set(selection.item.kind === "service" ? "servicio" : "combo", selection.item.id);
  if (selection.barberId) params.set("profesional", selection.barberId);
  const query = params.toString();
  return `/${slug}/reservar${query ? `?${query}` : ""}`;
}

type SearchParams = Record<string, string | string[] | undefined>;

/** Lee lo elegido desde los parámetros del link. Ignora lo que no tiene forma de id. */
export function selectionFrom(params: SearchParams): Selection {
  const value = (key: string) => {
    const raw = params[key];
    const first = Array.isArray(raw) ? raw[0] : raw;
    return first && /^[0-9a-f-]{36}$/i.test(first) ? first : undefined;
  };
  const service = value("servicio");
  const combo = value("combo");
  return {
    branchId: value("sucursal"),
    item: service ? { kind: "service", id: service } : combo ? { kind: "combo", id: combo } : undefined,
    barberId: value("profesional"),
  };
}

/** Lo necesario para volver a reservar lo mismo: sucursal, servicio o combo, y profesional. */
export function repeatHref(entry: AccountAppointment): string {
  const appointment = entry.appointment;
  const item: BookableItem = appointment.comboId
    ? { kind: "combo", id: appointment.comboId }
    : { kind: "service", id: appointment.lines[0]!.serviceId };
  return bookingHref(entry.slug, { branchId: appointment.branchId, item, barberId: appointment.barberId });
}

/** Nombre, precio "desde" y profesionales de un servicio o combo del catálogo. */
export function offerOf(
  catalog: Catalog,
  item: BookableItem,
): { name: string; fromPrice: number; barbers: BarberTerms[] } | null {
  const found =
    item.kind === "service"
      ? catalog.services.find((service) => service.id === item.id)
      : catalog.combos.find((combo) => combo.id === item.id);
  return found ? { name: found.name, fromPrice: found.fromPrice, barbers: found.barbers } : null;
}

/** Quien atiende en la sucursal (sin sucursales asignadas, atiende en todas: es el dueño). */
export function worksAt(professional: Professional, branchId: string): boolean {
  return professional.branchIds.length === 0 || professional.branchIds.includes(branchId);
}

export type Choice = BarberTerms & { photoUrl: string | null };

/** Los profesionales que hacen lo elegido en esa sucursal, del más barato al más caro. */
export function choicesFor(
  catalog: Catalog,
  professionals: Professional[],
  item: BookableItem,
  branchId: string,
): Choice[] {
  const offer = offerOf(catalog, item);
  if (!offer) {
    return [];
  }
  const byId = new Map(professionals.map((professional) => [professional.barberId, professional]));
  return offer.barbers
    .filter((terms) => {
      const professional = byId.get(terms.barberId);
      return professional !== undefined && worksAt(professional, branchId);
    })
    .map((terms) => ({ ...terms, photoUrl: byId.get(terms.barberId)?.photoUrl ?? null }));
}
