import type { Metadata } from "next";
import { notFound, permanentRedirect } from "next/navigation";
import { Suspense } from "react";

import { BookingFlow } from "@/components/booking/booking-flow";
import { getBusiness, getCatalog, getProfessionals } from "@/lib/api/server";
import { selectionFrom } from "@/lib/booking";
import { messages } from "@/messages/es-AR";

type Props = PageProps<"/[slug]/reservar">;

export async function generateMetadata({ params }: Props): Promise<Metadata> {
  const business = await getBusiness((await params).slug);
  return {
    title: business ? `${messages.booking.title} en ${business.name}` : messages.business.notFoundTitle,
    // El flujo de reserva no aporta a los buscadores: se indexa la página del negocio.
    robots: { index: false },
  };
}

export default function BookingRoute({ params, searchParams }: Props) {
  return (
    <main className="mx-auto flex w-full max-w-3xl flex-1 flex-col px-4 py-4">
      <Suspense fallback={<p className="py-6 text-muted-foreground">{messages.business.loading}</p>}>
        <BookingContent params={params} searchParams={searchParams} />
      </Suspense>
    </main>
  );
}

async function BookingContent({ params, searchParams }: Props) {
  const { slug } = await params;
  const business = await getBusiness(slug);
  if (!business) {
    notFound();
  }
  const query = await searchParams;
  if (business.canonicalSlug !== slug) {
    const rest = new URLSearchParams(
      Object.entries(query).flatMap(([key, value]) =>
        typeof value === "string" ? [[key, value]] : [],
      ),
    ).toString();
    permanentRedirect(`/${business.canonicalSlug}/reservar${rest ? `?${rest}` : ""}`);
  }
  const [catalog, professionals] = await Promise.all([getCatalog(slug), getProfessionals(slug)]);
  return (
    <BookingFlow
      slug={slug}
      business={business}
      catalog={catalog}
      professionals={professionals}
      initial={selectionFrom(query)}
    />
  );
}
