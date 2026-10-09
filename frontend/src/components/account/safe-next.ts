/**
 * A dónde volver después de ingresar. Solo rutas internas: un link armado por terceros no puede mandar a
 * la persona a otro sitio ("redirección abierta").
 */
export function safeNext(value: string | null, fallback = "/mis-turnos"): string {
  if (!value || !value.startsWith("/") || value.startsWith("//") || value.startsWith("/\\")) {
    return fallback;
  }
  return value;
}
