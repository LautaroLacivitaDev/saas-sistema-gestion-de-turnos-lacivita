import { expect, test } from "@playwright/test";

test("la dueña entra con la cuenta de ejemplo y ve la agenda", async ({ page, isMobile }) => {
  await page.goto("/demo");
  await page.getByRole("listitem").filter({ hasText: "duena.demo@example.com" }).getByRole("button", { name: "Entrar" }).click();

  await expect(page).toHaveURL(/\/panel\/.+\/agenda/);
  await expect(page.getByRole("heading", { name: "Agenda" })).toBeVisible();
  // Mobile first: Día por defecto en el celular, Semana desde tablet.
  await expect(page.getByRole("radiogroup", { name: "Agenda" }).getByRole("radio", { checked: true })).toHaveText(
    isMobile ? "Día" : "Semana",
  );

  await page.goto(page.url().replace(/\/agenda$/, "/equipo"));
  await expect(page.getByText("Nico Ríos")).toBeVisible();
});

test("las pantallas principales no tienen scroll horizontal en un celular chico", async ({ page }) => {
  await page.setViewportSize({ width: 320, height: 640 });
  for (const path of ["/", "/barberia-el-faro", "/barberia-el-faro/reservar", "/demo", "/demo/emails", "/ingresar"]) {
    await page.goto(path);
    await page.waitForLoadState("networkidle");
    const overflow = await page.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
    expect(overflow, path).toBeLessThanOrEqual(0);
  }
});
