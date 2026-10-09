import type { Branch, BusinessPage, Catalog } from "@/lib/api/types";

// Datos estructurados (schema.org en JSON-LD) de la página de un negocio, para que los buscadores muestren
// su nombre, dirección y servicios.

const TYPES: Record<string, string> = {
  BARBERSHOP: "HairSalon",
  HAIR_SALON: "HairSalon",
  BEAUTY_SALON: "BeautySalon",
  NAIL_SALON: "NailSalon",
};

function address(branch: Branch) {
  return {
    "@type": "PostalAddress",
    streetAddress: branch.street,
    addressLocality: branch.neighborhood ?? branch.city,
    addressRegion: branch.city,
    addressCountry: "AR",
  };
}

function place(branch: Branch) {
  return {
    "@type": "Place",
    name: branch.name,
    address: address(branch),
    ...(branch.latitude !== null && branch.longitude !== null
      ? { geo: { "@type": "GeoCoordinates", latitude: branch.latitude, longitude: branch.longitude } }
      : {}),
    ...(branch.phone ? { telephone: branch.phone } : {}),
  };
}

export function businessJsonLd(page: BusinessPage, catalog: Catalog, url: string) {
  const [main] = page.branches;
  return {
    "@context": "https://schema.org",
    "@type": TYPES[page.category] ?? "LocalBusiness",
    name: page.name,
    url,
    ...(page.description ? { description: page.description } : {}),
    ...(main ? { address: address(main) } : {}),
    ...(main?.phone ? { telephone: main.phone } : {}),
    ...(page.branches.length > 1 ? { location: page.branches.map(place) } : {}),
    makesOffer: catalog.services.map((service) => ({
      "@type": "Offer",
      itemOffered: { "@type": "Service", name: service.name },
      priceSpecification: {
        "@type": "PriceSpecification",
        price: service.fromPrice,
        priceCurrency: "ARS",
        minPrice: service.fromPrice,
      },
    })),
  };
}

/** El JSON listo para un <script>: se escapa "<" para que un texto del negocio no pueda cerrar el script. */
export function serializeJsonLd(data: unknown): string {
  return JSON.stringify(data).replace(/</g, "\\u003c");
}
