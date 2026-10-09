import Link from "next/link";

import { SearchForm } from "@/components/search-form";
import { messages } from "@/messages/es-AR";

export default function Home() {
  const t = messages.home;

  return (
    <main className="flex flex-1 flex-col items-center px-4 py-12 sm:py-20">
      <div className="flex w-full max-w-xl flex-col gap-6 text-center">
        <div className="flex flex-col gap-3">
          <h1 className="text-3xl font-semibold tracking-tight text-balance sm:text-5xl">{t.title}</h1>
          <p className="text-base text-pretty text-muted-foreground sm:text-lg">{t.subtitle}</p>
        </div>
        <SearchForm />
        <Link href="/buscar" className="mx-auto flex h-11 items-center text-base underline underline-offset-4">
          {t.browseAll}
        </Link>
        <p className="text-sm text-muted-foreground">
          {t.alreadyBooked}{" "}
          <Link href="/mis-turnos" className="inline-flex h-11 items-center underline underline-offset-4">
            {t.seeMyAppointments}
          </Link>
        </p>
      </div>
    </main>
  );
}
