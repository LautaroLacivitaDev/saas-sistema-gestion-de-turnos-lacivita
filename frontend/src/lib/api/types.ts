// Tipos de la API, tomados del OpenAPI del backend (schema.d.ts, generado con `npm run api:types`).
//
// El OpenAPI no marca qué campos pueden venir en null: los tipos se generan como obligatorios y acá se
// declaran, en un solo lugar, los que el backend deja vacíos.
import type { components } from "./schema";

type Schemas = components["schemas"];

/** Cambia el tipo de algunos campos a "o null". */
type Nullable<T, K extends keyof T> = Omit<T, K> & { [P in K]: T[P] | null };

export type SearchResults = Omit<Schemas["SearchResultsView"], "items"> & { items: SearchHit[] };
export type SearchHit = Nullable<Schemas["SearchHitView"], "description">;

export type Branch = Nullable<Schemas["Branch"], "neighborhood" | "latitude" | "longitude" | "phone">;
export type BusinessPage = Omit<Nullable<Schemas["PublicPage"], "description">, "branches"> & {
  branches: Branch[];
};

export type BarberTerms = Schemas["BarberTerms"];
export type PublicService = Nullable<Schemas["PublicService"], "description">;
export type PublicCombo = Schemas["PublicCombo"];
export type Catalog = { services: PublicService[]; combos: PublicCombo[] };

export type Professional = Nullable<Schemas["ProfessionalView"], "bio" | "photoUrl">;
export type ProfessionalService = Schemas["ProfessionalServiceView"];

export type WeeklyHours = Schemas["WeeklyHours"];
export type Availability = Schemas["Availability"];
export type Slot = Schemas["Slot"];

export type Hold = Schemas["Hold"];
export type Appointment = Omit<Nullable<Schemas["Appointment"], "comboId">, "customer"> & {
  customer: Customer | null;
};
export type Customer = Nullable<Schemas["Customer"], "email" | "phone">;
export type ManagedAppointment = Omit<Schemas["ManagedAppointment"], "appointment"> & {
  appointment: Appointment;
};
export type AccountAppointment = Omit<Schemas["AccountAppointment"], "appointment"> & {
  appointment: Appointment;
};

export type Account = Schemas["Account"];

/** Lo que se reserva: un servicio o un combo. */
export type BookableItem = { kind: "service"; id: string } | { kind: "combo"; id: string };
