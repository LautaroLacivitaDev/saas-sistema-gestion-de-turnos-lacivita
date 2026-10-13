import { expect, test } from "@playwright/test";

const BUSINESS = "/barberia-el-faro";

/** Un email distinto en cada corrida: el buzón de la demo es por dirección. */
function guestEmail(project: string) {
  return `e2e-${project}-${Date.now()}@example.com`;
}

test("un cliente reserva como invitado con el código del email", async ({ page, request }, info) => {
  await page.goto(BUSINESS);
  await expect(page.getByRole("heading", { level: 1, name: "Barbería El Faro" })).toBeVisible();
  await page.getByRole("link", { name: "Reservar turno" }).first().click();

  // Paso 1: sucursal y servicio.
  await page.getByRole("button", { name: /Palermo/ }).click();
  await page.getByRole("button", { name: /^Corte\b/ }).first().click();
  await page.getByRole("button", { name: "Continuar" }).click();

  // Paso 2: el primer día con horarios libres (hoy puede no tener más).
  const days = page.getByRole("radiogroup", { name: "Día" }).getByRole("radio");
  const times = page.getByRole("radiogroup", { name: "Horarios" }).getByRole("radio");
  for (let day = 0; day < 7; day++) {
    await days.nth(day).click();
    await expect(page.getByText("Buscando horarios…")).toBeHidden();
    if ((await times.count()) > 0) {
      break;
    }
  }
  // Al elegir el horario se guarda unos minutos y pasa solo a los datos.
  await times.first().click();
  await expect(page.getByRole("heading", { name: "Tus datos" })).toBeVisible();

  // Paso 3: datos y código. El código se lee del buzón de la demo, como lo haría la persona.
  const email = guestEmail(info.project.name);
  await page.getByLabel("Nombre y apellido").fill("Prueba de Punta a Punta");
  await page.getByLabel("Email").fill(email);
  await page.getByRole("button", { name: "Recibir código por email" }).click();
  await expect(page.getByText(`Te mandamos un código a ${email}`)).toBeVisible();

  const inbox = await request.get(`/api/public/demo/inbox?email=${encodeURIComponent(email)}`);
  const [message] = (await inbox.json()) as { text: string }[];
  const code = /\b(\d{6})\b/.exec(message.text)?.[1];
  expect(code).toBeDefined();

  await page.getByLabel("Código de 6 dígitos").fill(code!);
  await page.getByRole("button", { name: "Confirmar turno" }).click();
  await expect(page.getByRole("heading", { name: "¡Listo! Tu turno quedó confirmado" })).toBeVisible();
});
