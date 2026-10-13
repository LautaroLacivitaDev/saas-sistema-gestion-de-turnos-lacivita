// Prueba de carga: muchas personas intentan tomar EL MISMO horario a la vez. Tiene que quedarle a una sola
// (la restricción de exclusión de PostgreSQL) y el resto recibir 409, sin errores ni demoras grandes.
//
// Corre contra la demo (negocio "Barbería El Faro"). El backend tiene que tener un límite de intentos alto,
// porque todos los pedidos salen de la misma IP:
//   mvnw spring-boot:run -Dspring-boot.run.profiles=dev,demo -Dspring-boot.run.arguments=--app.rate-limit.requests-per-minute=100000
//   docker run --rm -i --add-host=host.docker.internal:host-gateway grafana/k6:1.3.0 run -e BASE_URL=http://host.docker.internal:8080 - < load/same-slot.js
import { check } from "k6";
import { Counter } from "k6/metrics";
import http from "k6/http";

const BASE_URL = __ENV.BASE_URL || "http://localhost:8080";
const SLUG = "barberia-el-faro";
const PALERMO = "d0000000-0000-7000-8000-000000000011";
const CORTE = "d0000000-0000-7000-8000-000000000301";
const NICO = "d0000000-0000-7000-8000-000000000103";
const PEOPLE = Number(__ENV.PEOPLE || 100);

const won = new Counter("holds_won");
const lost = new Counter("holds_lost");

export const options = {
  scenarios: {
    // Todos a la vez: cada persona intenta una sola vez.
    sameSlot: { executor: "per-vu-iterations", vus: PEOPLE, iterations: 1, maxDuration: "1m" },
  },
  thresholds: {
    holds_won: ["count==1"],
    holds_lost: [`count==${PEOPLE - 1}`],
    // 409 es la respuesta esperada para quien llega tarde: no cuenta como error.
    http_req_failed: ["rate==0"],
    "http_req_duration{name:hold}": ["p(95)<2000"],
  },
};

/** Un horario libre de Nico el próximo miércoles: el mismo para todos. */
export function setup() {
  const today = new Date();
  const wednesday = new Date(today);
  wednesday.setUTCDate(today.getUTCDate() + ((3 - today.getUTCDay() + 7) % 7 || 7));
  const date = wednesday.toISOString().slice(0, 10);
  const response = http.get(
    `${BASE_URL}/api/public/businesses/${SLUG}/availability?branchId=${PALERMO}&serviceId=${CORTE}&barberId=${NICO}&date=${date}`,
  );
  const slots = response.json("slots");
  if (!slots || slots.length === 0) {
    throw new Error(`Nico no tiene horarios libres el ${date}`);
  }
  // Uno del medio del día, para no chocar con lo que hayan reservado otras corridas.
  return { startsAt: slots[Math.floor(slots.length / 2)].startsAt };
}

export default function (data) {
  // Cada persona tiene su propia cookie de CSRF y la repite en el encabezado, como hace el frontend.
  http.get(`${BASE_URL}/api/auth/csrf`);
  const csrf = http.cookieJar().cookiesForURL(BASE_URL)["XSRF-TOKEN"][0];
  const response = http.post(
    `${BASE_URL}/api/public/businesses/${SLUG}/holds`,
    JSON.stringify({ branchId: PALERMO, serviceId: CORTE, barberId: NICO, startsAt: data.startsAt }),
    {
      headers: { "Content-Type": "application/json", "X-XSRF-TOKEN": csrf },
      responseCallback: http.expectedStatuses(201, 409),
      tags: { name: "hold" },
    },
  );
  check(response, { "201 o 409": (r) => r.status === 201 || r.status === 409 });
  if (response.status === 201) {
    won.add(1);
  } else if (response.status === 409) {
    lost.add(1);
  }
}
