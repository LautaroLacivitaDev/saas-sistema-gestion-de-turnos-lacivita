import Link from "next/link";

import { messages } from "@/messages/es-AR";

/** Barra superior: el nombre lleva al inicio; a la derecha, buscar y mis turnos. */
export function SiteHeader() {
  return (
    <header className="sticky top-0 z-20 border-b bg-background/95 backdrop-blur supports-[backdrop-filter]:bg-background/80">
      <nav className="mx-auto flex h-14 max-w-3xl items-center justify-between px-4">
        <Link href="/" className="flex h-11 items-center text-lg font-semibold tracking-tight">
          {messages.app.name}
        </Link>
        <div className="flex items-center gap-1">
          <Link
            href="/buscar"
            className="flex h-11 items-center rounded-lg px-3 text-base text-muted-foreground hover:bg-muted"
          >
            {messages.nav.search}
          </Link>
          <Link
            href="/mis-turnos"
            className="flex h-11 items-center rounded-lg px-3 text-base text-muted-foreground hover:bg-muted"
          >
            {messages.nav.myAppointments}
          </Link>
        </div>
      </nav>
    </header>
  );
}
