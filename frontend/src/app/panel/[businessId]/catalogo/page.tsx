import { Catalog } from "@/components/panel/catalog/catalog";
import { messages } from "@/messages/es-AR";

export const metadata = { title: messages.panel.catalog.title };

// Ver el layout del panel: se arma en el navegador según la sesión.
export const instant = false;

export default function CatalogPage() {
  return <Catalog />;
}
