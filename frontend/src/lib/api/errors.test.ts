import { describe, expect, it } from "vitest";

import { messages } from "@/messages/es-AR";

import { ApiError, apiErrorFrom, errorMessage } from "./errors";

describe("apiErrorFrom", () => {
  it("toma el código, el mensaje y los errores por campo de un Problem Details", async () => {
    const response = new Response(
      JSON.stringify({
        status: 400,
        code: "validation_failed",
        detail: "Revisá los datos ingresados.",
        errors: { email: "El email no tiene un formato válido." },
      }),
      { status: 400 },
    );

    const error = await apiErrorFrom(response);

    expect(error).toBeInstanceOf(ApiError);
    expect(error.code).toBe("validation_failed");
    expect(error.message).toBe("Revisá los datos ingresados.");
    expect(error.fieldErrors).toEqual({ email: "El email no tiene un formato válido." });
  });

  it("sin cuerpo, usa el estado y un mensaje genérico", async () => {
    const error = await apiErrorFrom(new Response("<html>Bad gateway</html>", { status: 502 }));

    expect(error.code).toBe("http_502");
    expect(error.message).toBe(messages.errors.generic);
  });
});

describe("errorMessage", () => {
  it("explica que no hay conexión cuando fetch no llega al servidor", () => {
    expect(errorMessage(new TypeError("Failed to fetch"))).toBe(messages.errors.network);
  });
});
