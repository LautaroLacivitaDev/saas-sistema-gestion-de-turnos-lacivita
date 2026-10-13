import { defineConfig, devices } from "@playwright/test";

/**
 * Pruebas de punta a punta contra la app corriendo en modo demo (backend con el perfil "dev,demo" y
 * frontend con NEXT_PUBLIC_DEMO_MODE=true): usan el negocio de ejemplo "Barbería El Faro" y leen los
 * códigos del buzón de la demo. Se corren en un celular y en escritorio, porque la app se usa sobre todo
 * desde el teléfono.
 */
export default defineConfig({
  testDir: "./e2e",
  fullyParallel: false,
  workers: 1,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [["github"], ["html", { open: "never" }]] : "list",
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://localhost:3000",
    locale: "es-AR",
    timezoneId: "America/Argentina/Buenos_Aires",
    trace: "retain-on-failure",
  },
  // En desarrollo Next compila cada página la primera vez: más margen que el de fábrica.
  timeout: 90_000,
  expect: { timeout: 15_000 },
  projects: [
    { name: "celular", use: { ...devices["Pixel 7"] } },
    { name: "escritorio", use: { ...devices["Desktop Chrome"] } },
  ],
});
