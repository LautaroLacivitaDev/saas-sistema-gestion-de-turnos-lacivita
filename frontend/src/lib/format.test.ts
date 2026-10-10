import { describe, expect, it } from "vitest";

import {
  addDays,
  dateTime,
  dayParts,
  initials,
  localDate,
  longDate,
  money,
  shortDateTime,
  time,
  zonedInstant,
} from "./format";

describe("money", () => {
  it("muestra pesos sin decimales cuando el importe es entero", () => {
    expect(money(9000).replace(/\s/g, " ")).toBe("$ 9.000");
  });

  it("muestra los centavos cuando los hay", () => {
    expect(money(9500.5).replace(/\s/g, " ")).toBe("$ 9.500,50");
  });
});

describe("fechas", () => {
  const ten = "2026-10-09T13:00:00Z";

  it("muestra la hora de la sucursal, no la del celular", () => {
    expect(time(ten, "America/Argentina/Buenos_Aires")).toBe("10:00");
    expect(dateTime(ten, "America/Argentina/Buenos_Aires")).toBe(
      "viernes 9 de octubre a las 10:00",
    );
  });

  it("calcula el día local de un instante en una zona", () => {
    const lateNight = new Date("2026-10-10T02:30:00Z");
    expect(localDate(lateNight, "America/Argentina/Buenos_Aires")).toBe(
      "2026-10-09",
    );
    expect(localDate(lateNight, "UTC")).toBe("2026-10-10");
  });

  it("pasa la hora de la sucursal al instante en UTC", () => {
    expect(
      zonedInstant("2026-10-09", "10:00", "America/Argentina/Buenos_Aires"),
    ).toBe("2026-10-09T13:00:00.000Z");
    // Nueva York cambia de hora: en julio está a -4 y en enero a -5.
    expect(zonedInstant("2026-07-01", "09:30", "America/New_York")).toBe(
      "2026-07-01T13:30:00.000Z",
    );
    expect(zonedInstant("2026-01-15", "09:30", "America/New_York")).toBe(
      "2026-01-15T14:30:00.000Z",
    );
  });

  it("suma días cruzando meses y años", () => {
    expect(addDays("2026-10-31", 1)).toBe("2026-11-01");
    expect(addDays("2026-12-31", 1)).toBe("2027-01-01");
  });

  it("separa el día de la semana y el número para los botones", () => {
    expect(dayParts("2026-10-09")).toEqual({
      weekday: "vie",
      day: "9",
      month: "oct",
    });
  });
});

describe("initials", () => {
  it("usa la primera letra de las dos primeras palabras", () => {
    expect(initials("juan pérez gómez")).toBe("JP");
    expect(initials("Ana")).toBe("A");
  });
});

describe("shortDateTime y longDate", () => {
  it("muestran fechas cortas para listas y largas para días cerrados", () => {
    expect(
      shortDateTime("2026-10-20T17:00:00Z", "America/Argentina/Buenos_Aires"),
    ).toBe("mar 20 oct, 14:00");
    expect(longDate("2026-12-25")).toBe("viernes 25 de diciembre");
  });
});
