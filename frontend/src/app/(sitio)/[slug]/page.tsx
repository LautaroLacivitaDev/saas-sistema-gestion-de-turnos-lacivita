import type { Metadata } from "next";
import Link from "next/link";
import { notFound, permanentRedirect } from "next/navigation";
import { Suspense } from "react";

import { Avatar } from "@/components/avatar";
import { BottomBar, BottomBarSpacer } from "@/components/bottom-bar";
import { CopyLinkButton } from "@/components/copy-link-button";
import { QrCode } from "@/components/qr-code";
import { buttonVariants } from "@/components/ui/button";
import {
  getBranchHours,
  getBusiness,
  getCatalog,
  getProfessionals,
} from "@/lib/api/server";
import type { Branch, BusinessPage, Catalog, Professional, WeeklyHours } from "@/lib/api/types";
import { bookingHref } from "@/lib/booking";
import { money } from "@/lib/format";
import { businessJsonLd, serializeJsonLd } from "@/lib/jsonld";
import { absoluteUrl } from "@/lib/site";
import { messages } from "@/messages/es-AR";

const t = messages.business;

export async function generateMetadata({ params }: PageProps<"/[slug]">): Promise<Metadata> {
  const { slug } = await params;
  const business = await getBusiness(slug);
  if (!business) {
    return { title: t.notFoundTitle, robots: { index: false } };
  }
  const category = messages.categories[business.category] ?? "";
  const description =
    business.description ?? `${category} · ${business.branches.map((branch) => branch.city).join(", ")}`;
  const url = absoluteUrl(`/${business.canonicalSlug}`);
  return {
    title: business.name,
    description,
    alternates: { canonical: url },
    openGraph: { title: business.name, description, url, type: "website" },
    twitter: { card: "summary", title: business.name, description },
  };
}

export default function BusinessRoute({ params }: PageProps<"/[slug]">) {
  return (
    <Suspense fallback={<p className="px-4 py-6 text-muted-foreground">{t.loading}</p>}>
      <BusinessContent params={params} />
    </Suspense>
  );
}

async function BusinessContent({ params }: Pick<PageProps<"/[slug]">, "params">) {
  const { slug } = await params;
  const business = await getBusiness(slug);
  if (!business) {
    notFound();
  }
  if (business.canonicalSlug !== slug) {
    // Link viejo: el negocio cambió su dirección. Se redirige a la actual.
    permanentRedirect(`/${business.canonicalSlug}`);
  }
  const [catalog, professionals, hours] = await Promise.all([
    getCatalog(slug),
    getProfessionals(slug),
    Promise.all(business.branches.map((branch) => getBranchHours(slug, branch.id))),
  ]);
  const url = absoluteUrl(`/${slug}`);

  return (
    <main className="mx-auto flex w-full max-w-3xl flex-1 flex-col gap-8 px-4 py-6">
      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{ __html: serializeJsonLd(businessJsonLd(business, catalog, url)) }}
      />
      <Header business={business} />
      <Services catalog={catalog} />
      {professionals.length > 0 && <Team slug={slug} professionals={professionals} />}
      <Branches branches={business.branches} hours={hours} />
      <Share name={business.name} url={url} />
      <BottomBarSpacer />
      <BottomBar>
        <Link href={bookingHref(slug)} className={buttonVariants({ size: "touch", className: "w-full" })}>
          {t.book}
        </Link>
      </BottomBar>
    </main>
  );
}

function Header({ business }: { business: BusinessPage }) {
  return (
    <header className="flex flex-col gap-2">
      <p className="text-sm text-muted-foreground">{messages.categories[business.category]}</p>
      <h1 className="text-3xl font-semibold tracking-tight text-balance">{business.name}</h1>
      {business.description && <p className="text-pretty text-muted-foreground">{business.description}</p>}
    </header>
  );
}

function Services({ catalog }: { catalog: Catalog }) {
  if (catalog.services.length === 0 && catalog.combos.length === 0) {
    return <p className="text-muted-foreground">{t.noServices}</p>;
  }
  const durations = (minutes: number[]) => {
    const min = Math.min(...minutes);
    const max = Math.max(...minutes);
    return min === max ? t.minutes(min) : `${min}–${t.minutes(max)}`;
  };
  return (
    <section className="flex flex-col gap-3" aria-labelledby="servicios">
      <h2 id="servicios" className="text-xl font-semibold">
        {t.services}
      </h2>
      <ul className="divide-y rounded-xl border">
        {catalog.services.map((service) => (
          <li key={service.id} className="flex items-start justify-between gap-4 p-4">
            <div className="flex min-w-0 flex-col gap-0.5">
              <span className="font-medium">{service.name}</span>
              {service.description && (
                <span className="text-sm text-muted-foreground">{service.description}</span>
              )}
              <span className="text-sm text-muted-foreground">
                {durations(service.barbers.map((barber) => barber.durationMinutes))}
              </span>
            </div>
            <span className="shrink-0 text-sm font-medium">{t.from(money(service.fromPrice))}</span>
          </li>
        ))}
      </ul>
      {catalog.combos.length > 0 && (
        <>
          <h3 className="text-lg font-semibold">{t.combos}</h3>
          <ul className="divide-y rounded-xl border">
            {catalog.combos.map((combo) => (
              <li key={combo.id} className="flex items-start justify-between gap-4 p-4">
                <div className="flex min-w-0 flex-col gap-0.5">
                  <span className="font-medium">{combo.name}</span>
                  <span className="text-sm text-muted-foreground">
                    {durations(combo.barbers.map((barber) => barber.durationMinutes))}
                  </span>
                </div>
                <span className="shrink-0 text-sm font-medium">{t.from(money(combo.fromPrice))}</span>
              </li>
            ))}
          </ul>
        </>
      )}
    </section>
  );
}

function Team({ slug, professionals }: { slug: string; professionals: Professional[] }) {
  return (
    <section className="flex flex-col gap-3" aria-labelledby="profesionales">
      <h2 id="profesionales" className="text-xl font-semibold">
        {t.professionals}
      </h2>
      <ul className="grid gap-3 sm:grid-cols-2">
        {professionals.map((professional) => (
          <li key={professional.barberId}>
            <Link
              href={`/${slug}/profesionales/${professional.barberId}`}
              className="flex items-center gap-3 rounded-xl border p-3 transition-colors hover:bg-muted/50"
            >
              <Avatar name={professional.name} photoUrl={professional.photoUrl} />
              <span className="flex min-w-0 flex-col">
                <span className="font-medium">{professional.name}</span>
                {professional.specialties.length > 0 && (
                  <span className="truncate text-sm text-muted-foreground">
                    {professional.specialties.join(" · ")}
                  </span>
                )}
              </span>
            </Link>
          </li>
        ))}
      </ul>
    </section>
  );
}

const WEEK = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];

function Branches({ branches, hours }: { branches: Branch[]; hours: (WeeklyHours | null)[] }) {
  return (
    <section className="flex flex-col gap-3" aria-labelledby="sucursales">
      <h2 id="sucursales" className="text-xl font-semibold">
        {t.branches}
      </h2>
      <ul className="flex flex-col gap-3">
        {branches.map((branch, index) => (
          <li key={branch.id} className="flex flex-col gap-3 rounded-xl border p-4">
            <div className="flex flex-col gap-0.5">
              <span className="font-medium">{branch.name}</span>
              <span className="text-sm text-muted-foreground">
                {[branch.street, branch.neighborhood, branch.city].filter(Boolean).join(", ")}
              </span>
            </div>
            <WeekHours hours={hours[index] ?? null} />
            <div className="flex flex-wrap gap-2">
              <a
                href={mapsUrl(branch)}
                target="_blank"
                rel="noopener noreferrer"
                className={buttonVariants({ variant: "outline", size: "touch" })}
              >
                {t.directions}
              </a>
              {branch.phone && (
                <a href={`tel:${branch.phone}`} className={buttonVariants({ variant: "outline", size: "touch" })}>
                  {t.call}
                </a>
              )}
            </div>
          </li>
        ))}
      </ul>
    </section>
  );
}

function WeekHours({ hours }: { hours: WeeklyHours | null }) {
  if (!hours) {
    return null;
  }
  const byDay = new Map(hours.days.map((day) => [day.day as string, day.ranges]));
  return (
    <dl className="grid grid-cols-[auto_1fr] gap-x-4 gap-y-1 text-sm">
      {WEEK.map((day) => {
        const ranges = byDay.get(day) ?? [];
        return (
          <div key={day} className="contents">
            <dt className="text-muted-foreground">{messages.days[day]}</dt>
            <dd>
              {ranges.length === 0
                ? t.closed
                : ranges.map((range) => `${range.start.slice(0, 5)}–${range.end.slice(0, 5)}`).join(", ")}
            </dd>
          </div>
        );
      })}
    </dl>
  );
}

async function Share({ name, url }: { name: string; url: string }) {
  return (
    <section className="flex flex-col items-center gap-3 rounded-xl border p-4 text-center" aria-labelledby="compartir">
      <h2 id="compartir" className="text-lg font-semibold">
        {t.share}
      </h2>
      <p className="text-sm text-muted-foreground">{t.shareHint}</p>
      <QrCode value={url} label={t.qrAlt(name)} />
      <CopyLinkButton url={url} title={name} />
    </section>
  );
}

function mapsUrl(branch: Branch): string {
  const query =
    branch.latitude !== null && branch.longitude !== null
      ? `${branch.latitude},${branch.longitude}`
      : [branch.street, branch.neighborhood, branch.city, "Argentina"].filter(Boolean).join(", ");
  return `https://www.google.com/maps/search/?api=1&query=${encodeURIComponent(query)}`;
}
