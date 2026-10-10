import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";
import { Suspense } from "react";

import { Avatar } from "@/components/avatar";
import { BottomBar, BottomBarSpacer } from "@/components/bottom-bar";
import { buttonVariants } from "@/components/ui/button";
import { getBusiness, getProfessional } from "@/lib/api/server";
import { bookingHref } from "@/lib/booking";
import { money } from "@/lib/format";
import { absoluteUrl } from "@/lib/site";
import { messages } from "@/messages/es-AR";

const t = messages.professional;

type Props = PageProps<"/[slug]/profesionales/[barberId]">;

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const { slug, barberId } = await params;
  const [business, professional] = await Promise.all([getBusiness(slug), getProfessional(slug, barberId)]);
  if (!business || !professional) {
    return { title: t.notFoundTitle, robots: { index: false } };
  }
  const title = `${professional.name} en ${business.name}`;
  const description = professional.bio ?? professional.specialties.join(" · ");
  return {
    title,
    description,
    alternates: { canonical: absoluteUrl(`/${business.canonicalSlug}/profesionales/${barberId}`) },
    openGraph: {
      title,
      description,
      type: "profile",
      ...(professional.photoUrl ? { images: [professional.photoUrl] } : {}),
    },
  };
}

export default function ProfessionalRoute({ params }: Props) {
  return (
    <Suspense fallback={<p className="px-4 py-6 text-muted-foreground">{messages.business.loading}</p>}>
      <ProfessionalContent params={params} />
    </Suspense>
  );
}

async function ProfessionalContent({ params }: Pick<Props, "params">) {
  const { slug, barberId } = await params;
  const [business, professional] = await Promise.all([getBusiness(slug), getProfessional(slug, barberId)]);
  if (!business || !professional) {
    notFound();
  }
  const branches = business.branches.filter(
    (branch) => professional.branchIds.length === 0 || professional.branchIds.includes(branch.id),
  );

  return (
    <main className="mx-auto flex w-full max-w-3xl flex-1 flex-col gap-6 px-4 py-6">
      <Link href={`/${slug}`} className="flex h-11 items-center self-start text-sm text-muted-foreground">
        ← {t.back(business.name)}
      </Link>
      <header className="flex items-center gap-4">
        <Avatar name={professional.name} photoUrl={professional.photoUrl} size={88} />
        <div className="flex min-w-0 flex-col gap-1">
          <h1 className="text-2xl font-semibold tracking-tight">{professional.name}</h1>
          <p className="text-sm text-muted-foreground">{business.name}</p>
        </div>
      </header>
      {professional.bio && <p className="whitespace-pre-line text-pretty">{professional.bio}</p>}
      {professional.specialties.length > 0 && (
        <section className="flex flex-col gap-2" aria-labelledby="especialidades">
          <h2 id="especialidades" className="text-lg font-semibold">
            {t.specialties}
          </h2>
          <ul className="flex flex-wrap gap-2">
            {professional.specialties.map((specialty) => (
              <li key={specialty} className="rounded-full bg-muted px-3 py-1 text-sm">
                {specialty}
              </li>
            ))}
          </ul>
        </section>
      )}
      <section className="flex flex-col gap-2" aria-labelledby="servicios">
        <h2 id="servicios" className="text-lg font-semibold">
          {t.services}
        </h2>
        <ul className="divide-y rounded-xl border">
          {professional.services.map((service) => (
            <li key={service.serviceId} className="flex items-center justify-between gap-4 p-4">
              <span className="flex flex-col">
                <span className="font-medium">{service.name}</span>
                <span className="text-sm text-muted-foreground">
                  {messages.business.minutes(service.durationMinutes)}
                </span>
              </span>
              <span className="shrink-0 font-medium">{money(service.price)}</span>
            </li>
          ))}
        </ul>
      </section>
      <section className="flex flex-col gap-1" aria-labelledby="sucursales">
        <h2 id="sucursales" className="text-lg font-semibold">
          {t.worksAt}
        </h2>
        <p className="text-muted-foreground">{branches.map((branch) => branch.name).join(" · ")}</p>
      </section>
      <BottomBarSpacer />
      <BottomBar>
        <Link
          href={bookingHref(slug, {
            barberId,
            ...(branches.length === 1 ? { branchId: branches[0]!.id } : {}),
          })}
          className={buttonVariants({ size: "touch", className: "w-full" })}
        >
          {t.bookWith(professional.name)}
        </Link>
      </BottomBar>
    </main>
  );
}
