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

// --- Panel del negocio ---

/** Un negocio de la persona y su rol ahí: OWNER, MANAGER o BARBER. */
export type Membership = Schemas["Membership"];
export type Business = Nullable<Schemas["Business"], "description">;
export type Member = Schemas["Member"];
export type Invitation = Schemas["Invitation"];
export type TeamService = Nullable<
  Schemas["Service"],
  "description" | "priceMin" | "priceMax" | "proposedBy"
>;
export type Combo = Schemas["Combo"];
export type Offering = Nullable<Schemas["Offering"], "ownPrice" | "ownDurationMinutes" | "requestedPrice">;
export type PriceRequest = Nullable<Schemas["PriceRequest"], "priceMin" | "priceMax">;
export type BarberSchedule = Schemas["BranchHours"];
export type Holiday = Nullable<Schemas["Holiday"], "branchId">;
export type TimeBlock = Nullable<Schemas["TimeBlock"], "barberId" | "branchId" | "reason">;
export type ScheduleRules = Schemas["Rules"];
export type CustomerSummary = Nullable<
  Schemas["CustomerSummaryView"],
  "email" | "phone" | "lastAppointmentAt"
>;
export type CustomerDetail = Omit<
  Nullable<Schemas["CustomerDetailView"], "email" | "phone" | "notes" | "preferences">,
  "history"
> & { history: Appointment[] };
export type Delivery = Nullable<Schemas["DeliveryView"], "recipientUserId" | "sentAt">;
export type Notice = Schemas["NoticeView"];
export type Profile = Nullable<Schemas["ProfileView"], "bio" | "photoUrl">;

/** Página de resultados de la API del panel. */
export type Page<T> = { items: T[]; page: number; size: number; totalItems: number; totalPages: number };

export type Role = "OWNER" | "MANAGER" | "BARBER";
export type DayOfWeek = Schemas["DayData"]["day"];
export type Week = { days: { day: DayOfWeek; ranges: { start: string; end: string }[] }[] };

/** Lo que se reserva: un servicio o un combo. */
export type BookableItem = { kind: "service"; id: string } | { kind: "combo"; id: string };
