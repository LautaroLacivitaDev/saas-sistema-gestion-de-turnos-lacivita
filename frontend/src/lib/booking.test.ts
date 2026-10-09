import { describe, expect, it } from "vitest";

import type { AccountAppointment, Catalog, Professional } from "./api/types";
import { bookingHref, choicesFor, repeatHref, selectionFrom } from "./booking";

const CENTRO = "11111111-1111-1111-1111-111111111111";
const NORTE = "22222222-2222-2222-2222-222222222222";
const CORTE = "33333333-3333-3333-3333-333333333333";
const COMBO = "44444444-4444-4444-4444-444444444444";
const ANA = "55555555-5555-5555-5555-555555555555";
const BETO = "66666666-6666-6666-6666-666666666666";
const DUENA = "77777777-7777-7777-7777-777777777777";

const catalog: Catalog = {
  services: [
    {
      id: CORTE,
      name: "Corte",
      category: "Cortes",
      description: null,
      fromPrice: 8000,
      barbers: [
        { barberId: BETO, barberName: "Beto", price: 8000, durationMinutes: 30 },
        { barberId: ANA, barberName: "Ana", price: 9000, durationMinutes: 30 },
        { barberId: DUENA, barberName: "Dueña", price: 12000, durationMinutes: 30 },
      ],
    },
  ],
  combos: [],
};

function professional(barberId: string, name: string, branchIds: string[]): Professional {
  return { barberId, name, bio: null, specialties: [], photoUrl: null, branchIds, services: [] };
}

const professionals = [
  professional(ANA, "Ana", [CENTRO]),
  professional(BETO, "Beto", [NORTE]),
  professional(DUENA, "Dueña", []),
];

describe("bookingHref y selectionFrom", () => {
  it("arman y leen el link con lo ya elegido", () => {
    const href = bookingHref("barberia-sur", {
      branchId: CENTRO,
      item: { kind: "combo", id: COMBO },
      barberId: ANA,
    });

    expect(href).toBe(`/barberia-sur/reservar?sucursal=${CENTRO}&combo=${COMBO}&profesional=${ANA}`);
    const params = Object.fromEntries(new URL(href, "https://x.test").searchParams);
    expect(selectionFrom(params)).toEqual({
      branchId: CENTRO,
      item: { kind: "combo", id: COMBO },
      barberId: ANA,
    });
  });

  it("ignora valores que no son ids", () => {
    expect(selectionFrom({ servicio: "<script>", profesional: ["no-es-un-id"] })).toEqual({
      branchId: undefined,
      item: undefined,
      barberId: undefined,
    });
  });
});

describe("choicesFor", () => {
  it("ofrece solo a quienes atienden en la sucursal (el dueño, en todas)", () => {
    const choices = choicesFor(catalog, professionals, { kind: "service", id: CORTE }, CENTRO);

    expect(choices.map((choice) => choice.barberName)).toEqual(["Ana", "Dueña"]);
  });

  it("no ofrece nada para algo que no está en el catálogo", () => {
    expect(choicesFor(catalog, professionals, { kind: "combo", id: COMBO }, CENTRO)).toEqual([]);
  });
});

describe("repeatHref", () => {
  it("vuelve a reservar lo mismo, con el mismo profesional y en la misma sucursal", () => {
    const entry = {
      businessName: "Barbería Sur",
      slug: "barberia-sur",
      appointment: {
        id: "a",
        branchId: CENTRO,
        branchName: "Centro",
        timeZone: "America/Argentina/Buenos_Aires",
        barberId: ANA,
        barberName: "Ana",
        status: "COMPLETED",
        source: "WEB",
        startsAt: "2026-10-01T13:00:00Z",
        endsAt: "2026-10-01T13:30:00Z",
        totalPrice: 9000,
        lines: [{ serviceId: CORTE, serviceName: "Corte", price: 9000, durationMinutes: 30 }],
        comboId: null,
        customer: null,
      },
    } satisfies AccountAppointment;

    expect(repeatHref(entry)).toBe(`/barberia-sur/reservar?sucursal=${CENTRO}&servicio=${CORTE}&profesional=${ANA}`);
  });
});
