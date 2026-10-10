import Link from "next/link";

import { buttonVariants } from "@/components/ui/button";
import { messages } from "@/messages/es-AR";

export default function BusinessNotFound() {
  const t = messages.business;
  return (
    <main className="mx-auto flex w-full max-w-xl flex-1 flex-col items-center justify-center gap-4 px-4 py-12 text-center">
      <h1 className="text-2xl font-semibold">{t.notFoundTitle}</h1>
      <p className="text-muted-foreground">{t.notFoundText}</p>
      <Link href="/buscar" className={buttonVariants({ size: "touch" })}>
        {t.backToSearch}
      </Link>
    </main>
  );
}
