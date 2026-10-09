import { describe, expect, it } from "vitest";

import type { BusinessPage } from "./api/types";
import { businessJsonLd, serializeJsonLd } from "./jsonld";

const page: BusinessPage = {
  canonicalSlug: "barberia-sur",
  name: "Barbería Sur",
  category: "BARBERSHOP",
  description: "Cortes clásicos",
  branches: [
    {
      id: "b1",
      name: "Centro",
      street: "Av. Corrientes 1234",
      neighborhood: "Almagro",
      city: "CABA",
      latitude: null,
      longitude: null,
      phone: "11 4567-8901",
      timeZone: "America/Argentina/Buenos_Aires",
    },
  ],
};

describe("businessJsonLd", () => {
  it("describe el negocio como peluquería con su dirección y sus servicios", () => {
    const data = businessJsonLd(
      page,
      {
        services: [
          { id: "s1", name: "Corte", category: "Cortes", description: null, fromPrice: 8000, barbers: [] },
        ],
        combos: [],
      },
      "https://laciturnos.test/barberia-sur",
    );

    expect(data["@type"]).toBe("HairSalon");
    expect(data.address).toMatchObject({ streetAddress: "Av. Corrientes 1234", addressCountry: "AR" });
    expect(data.telephone).toBe("11 4567-8901");
    expect(data.makesOffer[0]).toMatchObject({ itemOffered: { name: "Corte" } });
  });
});

describe("serializeJsonLd", () => {
  it("un texto del negocio no puede cerrar el script", () => {
    const json = serializeJsonLd({ name: "</script><script>alert(1)</script>" });

    expect(json).not.toContain("</script>");
    expect(JSON.parse(json).name).toBe("</script><script>alert(1)</script>");
  });
});
