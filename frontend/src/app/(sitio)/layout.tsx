import { SiteHeader } from "@/components/site-header";

/** Sitio público: buscador, páginas de los negocios, reserva y la cuenta del cliente. */
export default function SiteLayout({ children }: LayoutProps<"/">) {
  return (
    <>
      <SiteHeader />
      {children}
    </>
  );
}
