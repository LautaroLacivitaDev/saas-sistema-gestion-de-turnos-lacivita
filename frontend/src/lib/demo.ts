// Modo demo (proyecto de portfolio): los emails no se envían, quedan en el buzón de la demo. Lo enciende
// NEXT_PUBLIC_DEMO_MODE=true, junto con el perfil "demo" del backend.

export const demoMode = process.env.NEXT_PUBLIC_DEMO_MODE === "true";

/** Contraseña de las cuentas de ejemplo: pública a propósito, para entrar a probar. */
export const DEMO_PASSWORD = "demo1234";

/** Las cuentas del negocio de ejemplo ("Barbería El Faro"), con a dónde lleva cada una al entrar. */
export const DEMO_ACCOUNTS = [
  { email: "duena.demo@example.com", role: "OWNER", next: "/panel" },
  { email: "gerente.demo@example.com", role: "MANAGER", next: "/panel" },
  { email: "nico.demo@example.com", role: "BARBER", next: "/panel" },
  { email: "cliente.demo@example.com", role: "CUSTOMER", next: "/mis-turnos" },
] as const;

export const DEMO_BUSINESS_SLUG = "barberia-el-faro";

export function inboxPath(email: string): string {
  return `/demo/emails?email=${encodeURIComponent(email.trim().toLowerCase())}`;
}
