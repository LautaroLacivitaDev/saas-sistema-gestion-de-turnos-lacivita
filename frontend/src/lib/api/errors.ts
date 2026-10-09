import { messages } from "@/messages/es-AR";

/**
 * Error de la API. El backend responde con Problem Details (RFC 9457): un `code` estable para decidir qué
 * hacer y un `detail` en español para mostrar.
 */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
    /** Errores por campo de un formulario, por ejemplo `{ email: "El email no tiene un formato válido." }`. */
    readonly fieldErrors: Record<string, string> = {},
  ) {
    super(message);
    this.name = "ApiError";
  }
}

type Problem = { code?: unknown; detail?: unknown; errors?: unknown };

/** Arma el error a partir de la respuesta. Si no es un Problem Details, usa un mensaje genérico. */
export async function apiErrorFrom(response: Response): Promise<ApiError> {
  let problem: Problem = {};
  try {
    problem = (await response.json()) as Problem;
  } catch {
    // Sin cuerpo JSON (por ejemplo, un error del proxy): alcanza con el estado.
  }
  const code = typeof problem.code === "string" ? problem.code : `http_${response.status}`;
  const detail = typeof problem.detail === "string" ? problem.detail : messages.errors.generic;
  const fieldErrors =
    problem.errors && typeof problem.errors === "object"
      ? Object.fromEntries(
          Object.entries(problem.errors as Record<string, unknown>).filter(
            (entry): entry is [string, string] => typeof entry[1] === "string",
          ),
        )
      : {};
  return new ApiError(response.status, code, detail, fieldErrors);
}

/** Mensaje para mostrarle a la persona, sea cual sea el error. */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  if (error instanceof TypeError) {
    // fetch tira TypeError cuando no hay red.
    return messages.errors.network;
  }
  return messages.errors.generic;
}
