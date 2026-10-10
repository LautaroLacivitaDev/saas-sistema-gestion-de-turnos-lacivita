import { describe, expect, it } from "vitest";

import { normalizeSlug, suggestSlug } from "./slug";

describe("suggestSlug", () => {
  it("saca tildes, símbolos y espacios repetidos del nombre", () => {
    expect(suggestSlug("Barbería  El Tano & Cía.")).toBe("barberia-el-tano-cia");
  });

  it("no pasa de 50 caracteres", () => {
    expect(suggestSlug("a".repeat(80))).toHaveLength(50);
  });
});

describe("normalizeSlug", () => {
  it("pasa a minúsculas y cambia espacios por guiones mientras se escribe", () => {
    expect(normalizeSlug("Mi Barberia")).toBe("mi-barberia");
  });
});
