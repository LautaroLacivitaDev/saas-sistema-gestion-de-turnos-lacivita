import type { Metadata } from "next";
import { Suspense } from "react";

import { PanelShell } from "@/components/panel/panel-shell";
import { messages } from "@/messages/es-AR";

export const metadata: Metadata = {
  title: { default: messages.panel.title, template: `%s · ${messages.panel.title}` },
  robots: { index: false },
};

// DECISIÓN: el panel se arma en el navegador después de saber quién ingresó (todo depende de la sesión y
// del rol). No hay contenido que prerenderizar ni navegación "instantánea" que validar.
export const instant = false;

export default function BusinessPanelLayout({ children, params }: LayoutProps<"/panel/[businessId]">) {
  return (
    <Suspense fallback={<p className="p-4 text-muted-foreground">{messages.panel.loading}</p>}>
      <Shell params={params}>{children}</Shell>
    </Suspense>
  );
}

async function Shell({ params, children }: Pick<LayoutProps<"/panel/[businessId]">, "params" | "children">) {
  const { businessId } = await params;
  return <PanelShell businessId={businessId}>{children}</PanelShell>;
}
