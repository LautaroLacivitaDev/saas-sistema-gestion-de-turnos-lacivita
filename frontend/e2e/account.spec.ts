import { expect, test } from "@playwright/test";

// Errores reportados al probar la demo (2026-10-14): no se podía elegir el servicio antes que la sucursal
// y no había forma de cancelar un turno desde "Mis turnos".

test("el servicio se puede elegir antes que la sucursal", async ({ page }) => {
  await page.goto("/barberia-el-faro/reservar");

  await page.getByRole("button", { name: /^Barba/ }).click();
  await expect(page.getByRole("button", { name: /^Barba/ })).toHaveAttribute("aria-pressed", "true");
  await expect(page.getByText("Elegí en qué sucursal.")).toBeVisible();

  await page.getByRole("button", { name: /Belgrano/ }).click();
  await expect(page.getByRole("button", { name: "Continuar" })).toBeEnabled();
});

test("un cliente con cuenta reserva y cancela su turno desde Mis turnos", async ({ page }) => {
  await page.goto("/demo");
  await page.getByRole("listitem").filter({ hasText: "cliente.demo@example.com" }).getByRole("button", { name: "Entrar" }).click();
  await expect(page).toHaveURL(/\/mis-turnos/);

  // Con la cuenta (email verificado) se reserva sin código.
  await page.goto("/barberia-el-faro/reservar?sucursal=d0000000-0000-7000-8000-000000000012&servicio=d0000000-0000-7000-8000-000000000304");
  const days = page.getByRole("radiogroup", { name: "Día" }).getByRole("radio");
  const times = page.getByRole("radiogroup", { name: "Horarios" }).getByRole("radio");
  for (let day = 1; day < 8; day++) {
    await days.nth(day).click();
    await expect(page.getByText("Buscando horarios…")).toBeHidden();
    if ((await times.count()) > 0) break;
  }
  await times.last().click();
  await page.getByRole("button", { name: "Confirmar turno" }).click();
  await expect(page.getByRole("heading", { name: /Listo/ })).toBeVisible();

  await page.goto("/mis-turnos");
  await page
    .getByRole("listitem")
    .filter({ hasText: "Perfilado de cejas" })
    .getByRole("button", { name: "Cambiar o cancelar" })
    .first()
    .click();
  await expect(page).toHaveURL(/\/turno\?token=/);
  await page.getByRole("button", { name: "Cancelar turno" }).click();
  await page.getByRole("button", { name: "Sí, cancelar" }).click();
  await expect(page.getByText("Tu turno quedó cancelado.")).toBeVisible();
  await page.getByRole("link", { name: "Volver a mis turnos" }).click();
  await expect(page).toHaveURL(/\/mis-turnos/);
});
