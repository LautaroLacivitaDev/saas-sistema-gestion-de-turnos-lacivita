"use client";

import Link from "next/link";

import { Notice } from "@/components/notice";
import { buttonVariants } from "@/components/ui/button";
import type { Role } from "@/lib/api/types";
import { usePanel } from "@/lib/panel";
import { messages } from "@/messages/es-AR";

/**
 * Secciones del panel que no son para todos los roles. La navegación ya no las muestra, pero se puede
 * llegar por la dirección: en ese caso se avisa en lugar de mostrar una pantalla que el servidor rechaza.
 * El control de acceso real sigue siendo el del servidor.
 */
export function RoleGate({ role, children }: { role: Role; children: React.ReactNode }) {
  const panel = usePanel();
  if (panel.can(role)) {
    return children;
  }
  return (
    <div className="flex flex-col items-start gap-4">
      <Notice>{messages.panel.sectionNoAccess}</Notice>
      <Link href={`/panel/${panel.businessId}/agenda`} className={buttonVariants({ size: "touch" })}>
        {messages.panel.goToAgenda}
      </Link>
    </div>
  );
}
