"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useState } from "react";

import { Field } from "@/components/field";
import { Notice } from "@/components/notice";
import { SelectField } from "@/components/select-field";
import { Button, buttonVariants } from "@/components/ui/button";
import { useAccount } from "@/lib/account";
import { api } from "@/lib/api/client";
import { ApiError, errorMessage } from "@/lib/api/errors";
import type { Branch, Business, TeamService, Week } from "@/lib/api/types";
import { money } from "@/lib/format";
import { absoluteUrl } from "@/lib/site";
import { suggestSlug } from "@/lib/slug";
import { messages } from "@/messages/es-AR";

import { HoursEditor } from "../schedule/hours-editor";
import { SlugField, useSlugCheck } from "../settings/slug-field";

const t = messages.panel.onboarding;

/** Horario con el que arranca una sucursal nueva: lunes a viernes de 9 a 19 y sábados de 9 a 14. */
const STARTING_WEEK: Week = {
  days: [
    ...(["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"] as const).map((day) => ({
      day,
      ranges: [{ start: "09:00", end: "19:00" }],
    })),
    { day: "SATURDAY", ranges: [{ start: "09:00", end: "14:00" }] },
  ],
};

type Progress = { business?: Business; branch?: Branch; week?: Week; services: TeamService[] };

/**
 * Alta guiada de un negocio, pensada para terminarse en pocos minutos desde el celular. Cada paso guarda
 * apenas se confirma: si la persona se va a la mitad, lo hecho queda y sigue desde el panel.
 */
export function Onboarding() {
  const account = useAccount();
  const [step, setStep] = useState(0);
  const [progress, setProgress] = useState<Progress>({ services: [] });
  const next = (changes: Partial<Progress> = {}) => {
    setProgress((current) => ({ ...current, ...changes }));
    setStep((current) => current + 1);
  };

  if (account.isPending) {
    return <p className="text-muted-foreground">{messages.panel.loading}</p>;
  }
  if (!account.data) {
    return (
      <div className="flex flex-col items-start gap-4">
        <p>{messages.history.signInPrompt}</p>
        <Link href="/ingresar?next=/panel/nuevo" className={buttonVariants({ size: "touch" })}>
          {messages.account.signIn}
        </Link>
      </div>
    );
  }
  const { business, branch, week, services } = progress;
  const total = t.steps.length;
  if (step >= total && business) {
    return <Done business={business} />;
  }
  return (
    <div className="flex flex-col gap-5">
      <div className="flex flex-col gap-2">
        <p className="text-sm text-muted-foreground">
          {t.step(step + 1, total)} · {t.steps[step]}
        </p>
        <div className="flex gap-1" aria-hidden>
          {t.steps.map((label, index) => (
            <span key={label} className={`h-1.5 flex-1 rounded-full ${index <= step ? "bg-primary" : "bg-muted"}`} />
          ))}
        </div>
      </div>
      {step === 0 && <BusinessStep onDone={(created) => next({ business: created })} />}
      {step === 1 && business && <BranchStep business={business} onDone={(created) => next({ branch: created })} />}
      {step === 2 && business && branch && <HoursStep business={business} branch={branch} onDone={(saved) => next({ week: saved })} />}
      {step === 3 && business && (
        <ServicesStep business={business} services={services} onChange={(list) => setProgress({ ...progress, services: list })} onDone={() => next()} />
      )}
      {step === 4 && business && branch && (
        <SelfStep business={business} branch={branch} week={week ?? STARTING_WEEK} services={services} userId={account.data.id} onDone={() => next()} />
      )}
      {step === 5 && business && branch && <TeamStep business={business} branch={branch} onDone={() => next()} />}
    </div>
  );
}

function StepTitle({ title, hint }: { title: string; hint?: string }) {
  return (
    <div className="flex flex-col gap-1">
      <h2 className="text-xl font-semibold">{title}</h2>
      {hint && <p className="text-sm text-muted-foreground">{hint}</p>}
    </div>
  );
}

function BusinessStep({ onDone }: { onDone: (business: Business) => void }) {
  const client = useQueryClient();
  const [name, setName] = useState("");
  const [slug, setSlug] = useState("");
  const [slugTouched, setSlugTouched] = useState(false);
  const [category, setCategory] = useState("BARBERSHOP");
  const check = useSlugCheck(slug);
  const create = useMutation({
    mutationFn: () => api<Business>("/api/businesses", { method: "POST", body: { name, slug, category } }),
    onSuccess: (business) => {
      void client.invalidateQueries({ queryKey: ["memberships"] });
      onDone(business);
    },
  });
  return (
    <form
      className="flex flex-col gap-4"
      onSubmit={(event) => {
        event.preventDefault();
        create.mutate();
      }}
    >
      <StepTitle title={t.businessTitle} />
      <Field
        id="onboarding-name"
        label={messages.panel.settings.name}
        autoComplete="organization"
        required
        maxLength={80}
        value={name}
        onChange={(event) => {
          setName(event.target.value);
          // El link se propone a partir del nombre hasta que la persona lo cambie a mano.
          if (!slugTouched) {
            setSlug(suggestSlug(event.target.value));
          }
        }}
      />
      <SelectField label={messages.panel.settings.category} value={category} onChange={(event) => setCategory(event.target.value)}>
        {Object.entries(messages.categories).map(([value, label]) => (
          <option key={value} value={value}>
            {label}
          </option>
        ))}
      </SelectField>
      <SlugField
        value={slug}
        onChange={(value) => {
          setSlugTouched(true);
          setSlug(value);
        }}
      />
      {create.isError && <Notice tone="error">{errorMessage(create.error)}</Notice>}
      <Button type="submit" size="touch" disabled={create.isPending || !name.trim() || !check.data?.available}>
        {t.next}
      </Button>
    </form>
  );
}

function BranchStep({ business, onDone }: { business: Business; onDone: (branch: Branch) => void }) {
  const b = messages.panel.branches;
  const [values, setValues] = useState({ name: "", street: "", neighborhood: "", city: "", phone: "" });
  const set = (field: keyof typeof values) => (event: React.ChangeEvent<HTMLInputElement>) =>
    setValues({ ...values, [field]: event.target.value });
  const create = useMutation({
    mutationFn: () =>
      api<Branch>(`/api/businesses/${business.id}/branches`, {
        method: "POST",
        body: { ...values, neighborhood: values.neighborhood.trim() || undefined, phone: values.phone.trim() || undefined },
      }),
    onSuccess: onDone,
  });
  const fieldErrors = create.error instanceof ApiError ? create.error.fieldErrors : {};
  return (
    <form
      className="flex flex-col gap-4"
      onSubmit={(event) => {
        event.preventDefault();
        create.mutate();
      }}
    >
      <StepTitle title={t.branchTitle} hint={t.branchHint} />
      <Field id="onboarding-branch-name" label={b.name} autoComplete="off" required maxLength={80} value={values.name} error={fieldErrors.name} onChange={set("name")} />
      <Field id="onboarding-street" label={b.street} hint={b.streetHint} autoComplete="street-address" required maxLength={150} value={values.street} error={fieldErrors.street} onChange={set("street")} />
      <Field id="onboarding-neighborhood" label={b.neighborhood} autoComplete="off" maxLength={80} value={values.neighborhood} onChange={set("neighborhood")} />
      <Field id="onboarding-city" label={b.city} autoComplete="address-level2" required maxLength={80} value={values.city} error={fieldErrors.city} onChange={set("city")} />
      <Field id="onboarding-phone" label={b.phone} type="tel" inputMode="tel" autoComplete="tel" maxLength={30} value={values.phone} error={fieldErrors.phone} onChange={set("phone")} />
      {create.isError && Object.keys(fieldErrors).length === 0 && <Notice tone="error">{errorMessage(create.error)}</Notice>}
      <Button type="submit" size="touch" disabled={create.isPending}>
        {t.next}
      </Button>
    </form>
  );
}

function HoursStep({ business, branch, onDone }: { business: Business; branch: Branch; onDone: (week: Week) => void }) {
  return (
    <div className="flex flex-col gap-4">
      <StepTitle title={t.hoursTitle} hint={messages.panel.branches.hoursHint} />
      <HoursEditor
        id="onboarding-hours"
        initial={STARTING_WEEK}
        onSave={async (week) => {
          const saved = await api<Week>(`/api/businesses/${business.id}/branches/${branch.id}/hours`, { method: "PUT", body: week });
          onDone(saved);
        }}
      />
    </div>
  );
}

function ServicesStep({
  business,
  services,
  onChange,
  onDone,
}: {
  business: Business;
  services: TeamService[];
  onChange: (services: TeamService[]) => void;
  onDone: () => void;
}) {
  const c = messages.panel.catalog;
  const [name, setName] = useState("");
  const [category, setCategory] = useState("General");
  const [minutes, setMinutes] = useState("30");
  const [price, setPrice] = useState("");
  const add = useMutation({
    mutationFn: () =>
      api<TeamService>(`/api/businesses/${business.id}/services`, {
        method: "POST",
        body: { name, category, baseDurationMinutes: Number(minutes), basePrice: Number(price) },
      }),
    onSuccess: (created) => {
      onChange([...services, created]);
      setName("");
      setPrice("");
    },
  });
  return (
    <div className="flex flex-col gap-4">
      <StepTitle title={t.servicesTitle} hint={t.servicesHint} />
      {services.length === 0 ? (
        <p className="text-muted-foreground">{t.noServices}</p>
      ) : (
        <ul className="divide-y rounded-xl border">
          {services.map((service) => (
            <li key={service.id} className="flex items-center justify-between gap-3 px-4 py-3">
              <span className="font-medium">{service.name}</span>
              <span className="text-sm text-muted-foreground">
                {service.baseDurationMinutes} min · {money(service.basePrice)}
              </span>
            </li>
          ))}
        </ul>
      )}
      <form
        className="flex flex-col gap-3 rounded-xl border p-4"
        onSubmit={(event) => {
          event.preventDefault();
          add.mutate();
        }}
      >
        <Field id="onboarding-service" label={c.name} autoComplete="off" required maxLength={80} value={name} onChange={(e) => setName(e.target.value)} />
        <Field id="onboarding-category" label={c.category} hint={c.categoryHint} autoComplete="off" required maxLength={40} value={category} onChange={(e) => setCategory(e.target.value)} />
        <div className="grid grid-cols-2 gap-3">
          <Field id="onboarding-minutes" label={c.duration} type="number" inputMode="numeric" min={5} max={480} step={5} required value={minutes} onChange={(e) => setMinutes(e.target.value)} />
          <Field id="onboarding-price" label={c.price} type="number" inputMode="decimal" min={0} required value={price} onChange={(e) => setPrice(e.target.value)} />
        </div>
        {add.isError && <Notice tone="error">{errorMessage(add.error)}</Notice>}
        <Button type="submit" size="touch" variant="outline" disabled={add.isPending}>
          {t.addService}
        </Button>
      </form>
      <Button size="touch" onClick={onDone}>
        {services.length === 0 ? t.skip : t.next}
      </Button>
    </div>
  );
}

/** El dueño que también atiende: ofrece servicios y toma el horario de la sucursal como propio. */
function SelfStep({
  business,
  branch,
  week,
  services,
  userId,
  onDone,
}: {
  business: Business;
  branch: Branch;
  week: Week;
  services: TeamService[];
  userId: string;
  onDone: () => void;
}) {
  const [chosen, setChosen] = useState(services.map((service) => service.id));
  const join = useMutation({
    mutationFn: async () => {
      const base = `/api/businesses/${business.id}/barbers/${userId}`;
      for (const serviceId of chosen) {
        await api(`${base}/services/${serviceId}`, { method: "PUT", body: { price: null, durationMinutes: null } });
      }
      await api(`${base}/schedule/${branch.id}`, { method: "PUT", body: week });
    },
    onSuccess: onDone,
  });
  return (
    <div className="flex flex-col gap-4">
      <StepTitle title={t.selfTitle} hint={t.selfHint} />
      {services.length > 0 && (
        <fieldset className="flex min-w-0 flex-col gap-2">
          <legend className="mb-1 text-sm font-medium">{messages.panel.profile.myServices}</legend>
          {services.map((service) => (
            <label key={service.id} className="flex min-h-11 items-center gap-3 rounded-lg border px-3">
              <input
                type="checkbox"
                className="size-5"
                checked={chosen.includes(service.id)}
                onChange={() =>
                  setChosen(chosen.includes(service.id) ? chosen.filter((id) => id !== service.id) : [...chosen, service.id])
                }
              />
              {service.name}
            </label>
          ))}
        </fieldset>
      )}
      {join.isError && <Notice tone="error">{errorMessage(join.error)}</Notice>}
      <Button size="touch" disabled={join.isPending || (services.length > 0 && chosen.length === 0)} onClick={() => join.mutate()}>
        {join.isPending ? messages.panel.saving : t.iAttend}
      </Button>
      <Button size="touch" variant="ghost" onClick={onDone}>
        {t.skip}
      </Button>
    </div>
  );
}

function TeamStep({ business, branch, onDone }: { business: Business; branch: Branch; onDone: () => void }) {
  const [email, setEmail] = useState("");
  const [sent, setSent] = useState<string[]>([]);
  const invite = useMutation({
    mutationFn: () =>
      api(`/api/businesses/${business.id}/invitations`, {
        method: "POST",
        body: { email, role: "BARBER", branchIds: [branch.id] },
      }),
    onSuccess: () => {
      setSent([...sent, email]);
      setEmail("");
    },
  });
  return (
    <div className="flex flex-col gap-4">
      <StepTitle title={t.teamTitle} hint={t.teamHint} />
      <form
        className="flex flex-col gap-3"
        onSubmit={(event) => {
          event.preventDefault();
          invite.mutate();
        }}
      >
        <Field id="onboarding-invite" label={messages.panel.team.email} type="email" inputMode="email" autoComplete="off" required value={email} onChange={(e) => setEmail(e.target.value)} />
        {invite.isError && <Notice tone="error">{errorMessage(invite.error)}</Notice>}
        {sent.map((address) => (
          <Notice key={address} tone="success">
            {t.invited(address)}
          </Notice>
        ))}
        <Button type="submit" size="touch" variant="outline" disabled={invite.isPending}>
          {messages.panel.team.invite}
        </Button>
      </form>
      <Button size="touch" onClick={onDone}>
        {sent.length === 0 ? t.skip : t.next}
      </Button>
    </div>
  );
}

function Done({ business }: { business: Business }) {
  const url = absoluteUrl(`/${business.slug}`);
  return (
    <div className="flex flex-col gap-4">
      <StepTitle title={t.doneTitle} />
      <p>
        {t.doneText}{" "}
        <a href={`/${business.slug}`} className="font-medium break-all underline underline-offset-4">
          {url}
        </a>
      </p>
      <Link href={`/panel/${business.id}/agenda`} className={buttonVariants({ size: "touch" })}>
        {t.finish}
      </Link>
    </div>
  );
}
