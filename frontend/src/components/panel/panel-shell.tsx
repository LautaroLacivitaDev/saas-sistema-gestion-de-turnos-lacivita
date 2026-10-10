"use client";

import { useQuery } from "@tanstack/react-query";
import {
  BellIcon,
  CalendarDaysIcon,
  ClockIcon,
  ContactIcon,
  MenuIcon,
  ScissorsIcon,
  SettingsIcon,
  StoreIcon,
  UserIcon,
  UsersIcon,
  type LucideIcon,
} from "lucide-react";
import Link from "next/link";
import { usePathname } from "next/navigation";
import { useMemo } from "react";

import { Notice } from "@/components/notice";
import { buttonVariants } from "@/components/ui/button";
import { useAccount } from "@/lib/account";
import { api } from "@/lib/api/client";
import type { Role } from "@/lib/api/types";
import {
  PanelProvider,
  atLeast,
  useBranches,
  useCatalog,
  useMemberships,
  useProfessionals,
  type PanelContext,
} from "@/lib/panel";
import { cn } from "@/lib/utils";
import { messages } from "@/messages/es-AR";

const t = messages.panel;

type Item = { href: string; label: string; icon: LucideIcon; role: Role; main: boolean };

/** Secciones del panel. En el celular, las principales van en la barra de abajo y el resto en "Más". */
function itemsFor(businessId: string): Item[] {
  const base = `/panel/${businessId}`;
  return [
    { href: `${base}/agenda`, label: t.nav.agenda, icon: CalendarDaysIcon, role: "BARBER", main: true },
    { href: `${base}/clientes`, label: t.nav.customers, icon: ContactIcon, role: "MANAGER", main: true },
    { href: `${base}/catalogo`, label: t.nav.catalog, icon: ScissorsIcon, role: "BARBER", main: true },
    { href: `${base}/equipo`, label: t.nav.team, icon: UsersIcon, role: "MANAGER", main: true },
    { href: `${base}/sucursales`, label: t.nav.branches, icon: StoreIcon, role: "MANAGER", main: false },
    { href: `${base}/horarios`, label: t.nav.schedules, icon: ClockIcon, role: "BARBER", main: false },
    { href: `${base}/perfil`, label: t.nav.profile, icon: UserIcon, role: "BARBER", main: false },
    { href: `${base}/avisos`, label: t.nav.notices, icon: BellIcon, role: "BARBER", main: false },
    { href: `${base}/ajustes`, label: t.nav.settings, icon: SettingsIcon, role: "OWNER", main: false },
  ];
}

/**
 * Marco de todas las pantallas del panel de un negocio. Verifica que la persona pertenezca al negocio y
 * carga lo que comparten las pantallas (sucursales, profesionales, catálogo).
 */
export function PanelShell({ businessId, children }: { businessId: string; children: React.ReactNode }) {
  const account = useAccount();
  const memberships = useMemberships();
  const membership = memberships.data?.find((candidate) => candidate.businessId === businessId);
  const branches = useBranches(businessId);
  const professionals = useProfessionals(membership?.slug);
  const catalog = useCatalog(membership?.slug);

  const context = useMemo<PanelContext | null>(() => {
    if (!membership || !account.data || !branches.data || !professionals.data || !catalog.data) {
      return null;
    }
    const names = new Map(professionals.data.map((professional) => [professional.barberId, professional.name]));
    return {
      businessId,
      membership,
      userId: account.data.id,
      branches: branches.data,
      professionals: professionals.data,
      catalog: catalog.data,
      professionalName: (userId) => names.get(userId) ?? "",
      can: (required) => atLeast(membership.role, required),
    };
  }, [businessId, membership, account.data, branches.data, professionals.data, catalog.data]);

  if (account.isPending || memberships.isPending) {
    return <p className="p-4 text-muted-foreground">{t.loading}</p>;
  }
  if (!account.data) {
    return (
      <div className="mx-auto flex max-w-md flex-col items-start gap-4 p-4">
        <p>{messages.history.signInPrompt}</p>
        <Link href={`/ingresar?next=/panel/${businessId}`} className={buttonVariants({ size: "touch" })}>
          {messages.account.signIn}
        </Link>
      </div>
    );
  }
  if (!membership) {
    return (
      <div className="mx-auto flex max-w-md flex-col items-start gap-4 p-4">
        <Notice tone="error">{t.noAccess}</Notice>
        <Link href="/panel" className={buttonVariants({ size: "touch" })}>
          {t.backToPanel}
        </Link>
      </div>
    );
  }
  if (!context) {
    return <p className="p-4 text-muted-foreground">{t.loading}</p>;
  }

  const items = itemsFor(businessId).filter((item) => context.can(item.role));
  return (
    <PanelProvider value={context}>
      <div className="flex min-h-dvh flex-col md:flex-row">
        <SideNav items={items} name={membership.businessName} />
        <div className="flex min-w-0 flex-1 flex-col">
          <TopBar businessId={businessId} name={membership.businessName} />
          <main className="mx-auto w-full max-w-5xl flex-1 px-4 pt-4 pb-28 md:pb-8">{children}</main>
        </div>
        <BottomNav businessId={businessId} items={items} />
      </div>
    </PanelProvider>
  );
}

function TopBar({ businessId, name }: { businessId: string; name: string }) {
  const unread = useQuery({
    queryKey: ["panel", businessId, "unread"],
    queryFn: () => api<{ unread: number }>(`/api/businesses/${businessId}/notices/unread-count`),
    refetchInterval: 60_000,
  });
  const count = unread.data?.unread ?? 0;
  return (
    <header className="sticky top-0 z-20 flex h-14 items-center justify-between gap-2 border-b bg-background/95 px-4 backdrop-blur md:hidden">
      <Link href="/panel" className="min-w-0 truncate text-lg font-semibold">
        {name}
      </Link>
      <Link
        href={`/panel/${businessId}/avisos`}
        aria-label={count > 0 ? t.notices.unread(count) : t.notices.title}
        className="relative flex size-11 shrink-0 items-center justify-center rounded-lg hover:bg-muted"
      >
        <BellIcon aria-hidden className="size-5" />
        {count > 0 && (
          <span className="absolute top-1.5 right-1.5 flex min-w-5 items-center justify-center rounded-full bg-destructive px-1 text-xs font-medium text-white">
            {count > 9 ? "9+" : count}
          </span>
        )}
      </Link>
    </header>
  );
}

/** Desde tablet: barra lateral con todas las secciones. */
function SideNav({ items, name }: { items: Item[]; name: string }) {
  const pathname = usePathname();
  return (
    <nav aria-label={t.title} className="sticky top-0 hidden h-dvh w-60 shrink-0 flex-col gap-1 border-r p-3 md:flex">
      <Link href="/panel" className="mb-3 truncate px-3 py-2 text-lg font-semibold">
        {name}
      </Link>
      {items.map((item) => (
        <NavLink key={item.href} item={item} active={pathname.startsWith(item.href)} layout="side" />
      ))}
      <Link href="/panel" className="mt-auto flex h-11 items-center rounded-lg px-3 text-sm text-muted-foreground hover:bg-muted">
        {t.switchBusiness}
      </Link>
    </nav>
  );
}

/** En el celular: barra de abajo con las secciones principales y "Más", al alcance del pulgar. */
function BottomNav({ businessId, items }: { businessId: string; items: Item[] }) {
  const pathname = usePathname();
  const main = items.filter((item) => item.main);
  const more: Item = { href: `/panel/${businessId}/mas`, label: t.nav.more, icon: MenuIcon, role: "BARBER", main: true };
  const inMore = !main.some((item) => pathname.startsWith(item.href));
  return (
    <nav
      aria-label={t.title}
      className="fixed inset-x-0 bottom-0 z-20 grid border-t bg-background/95 pb-[env(safe-area-inset-bottom)] backdrop-blur md:hidden"
      style={{ gridTemplateColumns: `repeat(${main.length + 1}, minmax(0, 1fr))` }}
    >
      {main.map((item) => (
        <NavLink key={item.href} item={item} active={pathname.startsWith(item.href)} layout="bottom" />
      ))}
      <NavLink item={more} active={inMore} layout="bottom" />
    </nav>
  );
}

function NavLink({ item, active, layout }: { item: Item; active: boolean; layout: "side" | "bottom" }) {
  const Icon = item.icon;
  return (
    <Link
      href={item.href}
      aria-current={active ? "page" : undefined}
      className={cn(
        layout === "side"
          ? "flex h-11 items-center gap-3 rounded-lg px-3 text-base hover:bg-muted"
          : "flex min-h-14 flex-col items-center justify-center gap-0.5 text-xs",
        active ? "font-medium text-foreground" : "text-muted-foreground",
        active && layout === "side" && "bg-muted",
      )}
    >
      <Icon aria-hidden className="size-5" />
      <span className="truncate">{item.label}</span>
    </Link>
  );
}
