"use client";

import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";

import { Field } from "@/components/field";
import { Notice } from "@/components/notice";
import { SelectField } from "@/components/select-field";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import type { Business } from "@/lib/api/types";
import { panelKey, usePanel } from "@/lib/panel";
import { messages } from "@/messages/es-AR";

import { SlugField, useSlugCheck } from "./slug-field";

const t = messages.panel.settings;

type Template = { type: string; subject: string; body: string; custom: boolean };
type Templates = { templates: Template[]; variables: { key: string; description: string }[] };

/** Ajustes del negocio. Solo el dueño. */
export function Settings() {
  return (
    <div className="flex flex-col gap-8">
      <h1 className="text-2xl font-semibold tracking-tight">{t.title}</h1>
      <BusinessSection />
      <CancellationSection />
      <RemindersSection />
      <EmailsSection />
    </div>
  );
}

function Section({ id, title, hint, children }: { id: string; title: string; hint?: string; children: React.ReactNode }) {
  return (
    <section aria-labelledby={id} className="flex flex-col gap-4">
      <div className="flex flex-col gap-1">
        <h2 id={id} className="text-lg font-semibold">
          {title}
        </h2>
        {hint && <p className="text-sm text-muted-foreground">{hint}</p>}
      </div>
      {children}
    </section>
  );
}

/** Carga un dato y muestra el formulario cuando llega, con su estado inicial. */
function Loaded<T>({
  query,
  children,
}: {
  query: { isPending: boolean; isError: boolean; error: unknown; data: T | undefined };
  children: (data: T) => React.ReactNode;
}) {
  if (query.isPending) {
    return <p className="text-muted-foreground">{messages.panel.loading}</p>;
  }
  if (query.isError || query.data === undefined) {
    return <Notice tone="error">{errorMessage(query.error)}</Notice>;
  }
  return children(query.data);
}

function BusinessSection() {
  const panel = usePanel();
  const business = useQuery({
    queryKey: panelKey(panel.businessId, "business"),
    queryFn: () => api<Business>(`/api/businesses/${panel.businessId}`),
  });
  return (
    <Section id="negocio" title={t.business}>
      <Loaded query={business}>
        {(data) => (
          <>
            <ProfileForm business={data} />
            <SlugForm business={data} />
          </>
        )}
      </Loaded>
    </Section>
  );
}

function ProfileForm({ business }: { business: Business }) {
  const panel = usePanel();
  const client = useQueryClient();
  const [values, setValues] = useState({
    name: business.name,
    category: business.category,
    description: business.description ?? "",
    searchable: business.searchable,
  });
  const save = useMutation({
    mutationFn: () =>
      api<Business>(`/api/businesses/${panel.businessId}/profile`, {
        method: "PUT",
        body: { ...values, description: values.description.trim() || undefined },
      }),
    onSuccess: (updated) => {
      client.setQueryData(panelKey(panel.businessId, "business"), updated);
      void client.invalidateQueries({ queryKey: ["memberships"] });
    },
  });
  return (
    <form
      className="flex flex-col gap-4"
      onSubmit={(event) => {
        event.preventDefault();
        save.mutate();
      }}
    >
      <Field
        id="business-name"
        label={t.name}
        autoComplete="organization"
        required
        maxLength={80}
        value={values.name}
        onChange={(event) => setValues({ ...values, name: event.target.value })}
      />
      <SelectField label={t.category} value={values.category} onChange={(event) => setValues({ ...values, category: event.target.value })}>
        {Object.entries(messages.categories).map(([value, label]) => (
          <option key={value} value={value}>
            {label}
          </option>
        ))}
      </SelectField>
      <label htmlFor="business-description" className="flex flex-col gap-1.5">
        <span className="text-sm font-medium">{t.description}</span>
        <textarea
          id="business-description"
          rows={4}
          maxLength={1000}
          value={values.description}
          onChange={(event) => setValues({ ...values, description: event.target.value })}
          className="rounded-lg border border-input bg-background px-3 py-2 text-base outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
        />
        <span className="text-sm text-muted-foreground">{t.descriptionHint}</span>
      </label>
      <label className="flex min-h-11 items-center gap-3">
        <input
          type="checkbox"
          className="size-5"
          checked={values.searchable}
          onChange={(event) => setValues({ ...values, searchable: event.target.checked })}
        />
        {t.searchable}
      </label>
      {save.isError && <Notice tone="error">{errorMessage(save.error)}</Notice>}
      {save.isSuccess && <Notice tone="success">{messages.panel.saved}</Notice>}
      <Button type="submit" size="touch" className="self-start" disabled={save.isPending}>
        {save.isPending ? messages.panel.saving : messages.panel.save}
      </Button>
    </form>
  );
}

function SlugForm({ business }: { business: Business }) {
  const panel = usePanel();
  const client = useQueryClient();
  const [slug, setSlug] = useState(business.slug);
  const check = useSlugCheck(slug, business.slug);
  const save = useMutation({
    mutationFn: () => api<Business>(`/api/businesses/${panel.businessId}/slug`, { method: "PUT", body: { slug } }),
    onSuccess: (updated) => {
      client.setQueryData(panelKey(panel.businessId, "business"), updated);
      void client.invalidateQueries({ queryKey: ["memberships"] });
    },
  });
  const unchanged = slug.trim() === business.slug;
  return (
    <form
      className="flex flex-col gap-3 border-t pt-4"
      onSubmit={(event) => {
        event.preventDefault();
        save.mutate();
      }}
    >
      <SlugField value={slug} onChange={setSlug} current={business.slug} />
      {save.isError && <Notice tone="error">{errorMessage(save.error)}</Notice>}
      {save.isSuccess && <Notice tone="success">{messages.panel.saved}</Notice>}
      <Button
        type="submit"
        size="touch"
        variant="outline"
        className="self-start"
        disabled={save.isPending || unchanged || !check.data?.available}
      >
        {t.saveLink}
      </Button>
    </form>
  );
}

function CancellationSection() {
  const panel = usePanel();
  const url = `/api/businesses/${panel.businessId}/booking-settings`;
  const settings = useQuery({
    queryKey: panelKey(panel.businessId, "booking-settings"),
    queryFn: () => api<{ cancellationNoticeHours: number }>(url),
  });
  return (
    <Section id="cancelaciones" title={t.cancellation} hint={t.cancellationHint}>
      <Loaded query={settings}>
        {(data) => (
          <NumbersForm
            url={url}
            fields={[{ id: "cancellation-hours", label: t.cancellationHours, initial: data.cancellationNoticeHours, min: 0 }]}
            body={([hours]) => ({ cancellationNoticeHours: hours })}
          />
        )}
      </Loaded>
    </Section>
  );
}

function RemindersSection() {
  const panel = usePanel();
  const url = `/api/businesses/${panel.businessId}/notification-settings`;
  const settings = useQuery({
    queryKey: panelKey(panel.businessId, "notification-settings"),
    queryFn: () => api<{ reminderHours: number[] }>(url),
  });
  return (
    <Section id="recordatorios" title={t.reminders} hint={t.remindersHint}>
      <Loaded query={settings}>
        {(data) => (
          <NumbersForm
            url={url}
            optional
            fields={[0, 1, 2].map((index) => ({
              id: `reminder-${index}`,
              label: t.reminder(index + 1),
              initial: data.reminderHours[index] ?? null,
              min: 1,
            }))}
            body={(hours) => ({ reminderHours: hours.filter((value): value is number => value !== null) })}
          />
        )}
      </Loaded>
    </Section>
  );
}

/** Formulario de uno o más números enteros de horas (0 a 168). Los opcionales pueden quedar vacíos. */
function NumbersForm({
  url,
  fields,
  body,
  optional = false,
}: {
  url: string;
  fields: { id: string; label: string; initial: number | null; min: number }[];
  body: (values: (number | null)[]) => unknown;
  optional?: boolean;
}) {
  const [values, setValues] = useState(fields.map((field) => (field.initial === null ? "" : String(field.initial))));
  const save = useMutation({
    mutationFn: () =>
      api(url, { method: "PUT", body: body(values.map((value) => (value === "" ? null : Number(value)))) }),
  });
  return (
    <form
      className="flex flex-col gap-4"
      onSubmit={(event) => {
        event.preventDefault();
        save.mutate();
      }}
    >
      <div className="grid gap-3 sm:grid-cols-3">
        {fields.map((field, index) => (
          <Field
            key={field.id}
            id={field.id}
            label={field.label}
            type="number"
            inputMode="numeric"
            min={field.min}
            max={168}
            required={!optional}
            value={values[index]}
            onChange={(event) => {
              save.reset();
              setValues(values.map((value, i) => (i === index ? event.target.value : value)));
            }}
          />
        ))}
      </div>
      {save.isError && <Notice tone="error">{errorMessage(save.error)}</Notice>}
      {save.isSuccess && <Notice tone="success">{messages.panel.saved}</Notice>}
      <Button type="submit" size="touch" className="self-start" disabled={save.isPending}>
        {save.isPending ? messages.panel.saving : messages.panel.save}
      </Button>
    </form>
  );
}

function EmailsSection() {
  const panel = usePanel();
  const templates = useQuery({
    queryKey: panelKey(panel.businessId, "message-templates"),
    queryFn: () => api<Templates>(`/api/businesses/${panel.businessId}/message-templates`),
  });
  return (
    <Section id="emails" title={t.emails} hint={t.emailsHint}>
      <Loaded query={templates}>
        {(data) => (
          <>
            <p className="text-sm text-muted-foreground">
              {t.variables}{" "}
              {data.variables.map((variable, index) => (
                <span key={variable.key}>
                  {index > 0 && ", "}
                  <code className="rounded bg-muted px-1">{`{${variable.key}}`}</code> ({variable.description})
                </span>
              ))}
            </p>
            <ul className="flex flex-col gap-2">
              {data.templates.map((template) => (
                <li key={template.type}>
                  <TemplateEditor template={template} />
                </li>
              ))}
            </ul>
          </>
        )}
      </Loaded>
    </Section>
  );
}

function TemplateEditor({ template }: { template: Template }) {
  const panel = usePanel();
  const client = useQueryClient();
  const [subject, setSubject] = useState(template.subject);
  const [body, setBody] = useState(template.body);
  const url = `/api/businesses/${panel.businessId}/message-templates/${template.type}`;
  const refresh = () => client.invalidateQueries({ queryKey: panelKey(panel.businessId, "message-templates") });
  const save = useMutation({ mutationFn: () => api(url, { method: "PUT", body: { subject, body } }), onSuccess: refresh });
  const reset = useMutation({
    mutationFn: () => api<Template>(url, { method: "DELETE" }),
    onSuccess: (original) => {
      setSubject(original.subject);
      setBody(original.body);
      save.reset();
      void refresh();
    },
  });
  return (
    <details className="group rounded-xl border">
      <summary className="flex min-h-14 cursor-pointer list-none items-center justify-between gap-3 px-4">
        <span className="font-medium">{t.emailTypes[template.type] ?? template.type}</span>
        {template.custom && <span className="rounded-full bg-muted px-2 py-0.5 text-xs">{t.custom}</span>}
      </summary>
      <form
        className="flex flex-col gap-4 border-t p-4"
        onSubmit={(event) => {
          event.preventDefault();
          save.mutate();
        }}
      >
        <Field
          id={`subject-${template.type}`}
          label={t.subject}
          autoComplete="off"
          required
          maxLength={150}
          value={subject}
          onChange={(event) => setSubject(event.target.value)}
        />
        <label htmlFor={`body-${template.type}`} className="flex flex-col gap-1.5">
          <span className="text-sm font-medium">{t.body}</span>
          <textarea
            id={`body-${template.type}`}
            rows={6}
            required
            maxLength={2000}
            value={body}
            onChange={(event) => setBody(event.target.value)}
            className="rounded-lg border border-input bg-background px-3 py-2 text-base outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
          />
        </label>
        {(save.isError || reset.isError) && <Notice tone="error">{errorMessage(save.error ?? reset.error)}</Notice>}
        {save.isSuccess && <Notice tone="success">{messages.panel.saved}</Notice>}
        <div className="flex flex-wrap gap-2">
          <Button type="submit" size="touch" disabled={save.isPending}>
            {messages.panel.save}
          </Button>
          {template.custom && (
            <Button type="button" size="touch" variant="ghost" disabled={reset.isPending} onClick={() => reset.mutate()}>
              {t.resetTemplate}
            </Button>
          )}
        </div>
      </form>
    </details>
  );
}
