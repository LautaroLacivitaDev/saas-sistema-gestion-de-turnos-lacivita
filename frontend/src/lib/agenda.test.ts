import { describe, expect, it } from "vitest";

import { actionsFor, canMove, daysOf, periodOf, shift, weekStart } from "./agenda";

const TEN = "2026-10-09T13:00:00Z";
const BEFORE = new Date("2026-10-09T12:00:00Z").getTime();
const AFTER = new Date("2026-10-09T13:30:00Z").getTime();

describe("actionsFor", () => {
  it("antes de la hora, un turno confirmado se puede empezar o cancelar, pero no marcar como ausente", () => {
    expect(actionsFor("CONFIRMED", TEN, BEFORE)).toEqual(["IN_PROGRESS", "CANCELLED"]);
  });

  it("después de la hora, se puede terminar o marcar que no vino", () => {
    expect(actionsFor("CONFIRMED", TEN, AFTER)).toEqual(["IN_PROGRESS", "COMPLETED", "NO_SHOW", "CANCELLED"]);
    expect(actionsFor("PENDING", TEN, AFTER)).toEqual(["CONFIRMED", "NO_SHOW", "CANCELLED"]);
  });

  it("un turno terminado, cancelado o ausente ya no cambia", () => {
    for (const status of ["COMPLETED", "CANCELLED", "NO_SHOW"]) {
      expect(actionsFor(status, TEN, AFTER)).toEqual([]);
      expect(canMove(status)).toBe(false);
    }
  });
});

describe("períodos", () => {
  it("la semana va de lunes a domingo", () => {
    expect(weekStart("2026-10-09")).toBe("2026-10-05"); // viernes → lunes
    expect(weekStart("2026-10-11")).toBe("2026-10-05"); // domingo → lunes anterior
    expect(daysOf("week", "2026-10-09")).toHaveLength(7);
  });

  it("pide los turnos desde el principio hasta el final del día de la sucursal", () => {
    expect(periodOf("day", "2026-10-09", "America/Argentina/Buenos_Aires")).toEqual({
      from: "2026-10-09T03:00:00.000Z",
      to: "2026-10-10T03:00:00.000Z",
    });
  });

  it("avanza un día o una semana", () => {
    expect(shift("day", "2026-10-31", 1)).toBe("2026-11-01");
    expect(shift("week", "2026-10-09", -1)).toBe("2026-10-02");
  });
});
