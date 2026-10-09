import { describe, expect, it } from "vitest";

import { safeNext } from "./safe-next";

describe("safeNext", () => {
  it("vuelve a la página interna pedida", () => {
    expect(safeNext("/barberia-sur/reservar?servicio=x")).toBe("/barberia-sur/reservar?servicio=x");
  });

  it("nunca manda a otro sitio", () => {
    expect(safeNext("https://malicioso.test")).toBe("/mis-turnos");
    expect(safeNext("//malicioso.test")).toBe("/mis-turnos");
    expect(safeNext("/\\malicioso.test")).toBe("/mis-turnos");
    expect(safeNext(null)).toBe("/mis-turnos");
  });
});
