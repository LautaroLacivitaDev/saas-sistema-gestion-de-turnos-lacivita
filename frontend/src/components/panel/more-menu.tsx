"use client";

import { ChevronRightIcon } from "lucide-react";
import Link from "next/link";

import { usePanel } from "@/lib/panel";
import { messages } from "@/messages/es-AR";

const t = messages.panel;

/** En el celular, las secciones que no entran en la barra de abajo. */
export function MoreMenu() {
  const panel = usePanel();
  const base = `/panel/${panel.businessId}`;
  const links = [
    { href: `${base}/sucursales`, label: t.nav.branches, show: panel.can("MANAGER") },
    { href: `${base}/horarios`, label: t.nav.schedules, show: true },
    { href: `${base}/perfil`, label: t.nav.profile, show: true },
    { href: `${base}/avisos`, label: t.nav.notices, show: true },
    { href: `${base}/ajustes`, label: t.nav.settings, show: panel.can("OWNER") },
    { href: `/${panel.membership.slug}`, label: t.publicPage, show: true },
    { href: "/panel", label: t.switchBusiness, show: true },
  ].filter((link) => link.show);
  return (
    <ul className="divide-y rounded-xl border">
      {links.map((link) => (
        <li key={link.href}>
          <Link href={link.href} className="flex min-h-14 items-center justify-between px-4 text-base hover:bg-muted/50">
            {link.label}
            <ChevronRightIcon aria-hidden className="size-5 text-muted-foreground" />
          </Link>
        </li>
      ))}
    </ul>
  );
}
