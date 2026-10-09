import { apiErrorFrom } from "./errors";

// Llamadas a la API desde el navegador. Van al mismo dominio (/api/*) y Next las pasa al backend, así la
// cookie de sesión funciona sin CORS.
//
// CSRF en modo SPA: el backend deja el token en la cookie XSRF-TOKEN y espera recibirlo en el encabezado
// X-XSRF-TOKEN en todo lo que no sea GET. Si todavía no hay cookie, se pide una.

const CSRF_COOKIE = "XSRF-TOKEN";
const CSRF_HEADER = "X-XSRF-TOKEN";

function readCookie(name: string): string | null {
  const match = document.cookie.split("; ").find((cookie) => cookie.startsWith(`${name}=`));
  return match ? decodeURIComponent(match.slice(name.length + 1)) : null;
}

async function csrfToken(): Promise<string | null> {
  const existing = readCookie(CSRF_COOKIE);
  if (existing) {
    return existing;
  }
  await fetch("/api/auth/csrf", { credentials: "same-origin" });
  return readCookie(CSRF_COOKIE);
}

type Options = { method?: "GET" | "POST" | "PUT" | "DELETE"; body?: unknown };

/** Llama a la API. Si responde con error, tira un {@link ApiError} con el mensaje para mostrar. */
export async function api<T>(path: string, { method = "GET", body }: Options = {}): Promise<T> {
  const headers: Record<string, string> = { Accept: "application/json" };
  if (body !== undefined) {
    headers["Content-Type"] = "application/json";
  }
  if (method !== "GET") {
    const token = await csrfToken();
    if (token) {
      headers[CSRF_HEADER] = token;
    }
  }
  const response = await fetch(path, {
    method,
    headers,
    credentials: "same-origin",
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  if (!response.ok) {
    throw await apiErrorFrom(response);
  }
  if (response.status === 204 || response.headers.get("Content-Length") === "0") {
    return undefined as T;
  }
  const text = await response.text();
  return (text ? JSON.parse(text) : undefined) as T;
}
