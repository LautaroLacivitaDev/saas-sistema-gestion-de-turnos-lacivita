import type { Metadata } from "next";
import Link from "next/link";
import { notFound } from "next/navigation";

import { DemoAccounts } from "@/components/demo/demo-accounts";
import { buttonVariants } from "@/components/ui/button";
import { DEMO_BUSINESS_SLUG, DEMO_PASSWORD, demoMode } from "@/lib/demo";
import { messages } from "@/messages/es-AR";

export const metadata: Metadata = { title: messages.demo.title };

/** Cómo probar la demo: cuentas de ejemplo, el negocio de ejemplo y el buzón de emails. */
export default function DemoPage() {
  if (!demoMode) {
    notFound();
  }
  const t = messages.demo;
  return (
    <main className="mx-auto flex w-full max-w-xl flex-1 flex-col gap-6 px-4 py-8">
      <div className="flex flex-col gap-2">
        <h1 className="text-2xl font-semibold tracking-tight">{t.title}</h1>
        <p className="text-muted-foreground">{t.intro}</p>
      </div>
      <section aria-labelledby="cuentas" className="flex flex-col gap-3">
        <div className="flex flex-col gap-1">
          <h2 id="cuentas" className="text-lg font-semibold">
            {t.accounts}
          </h2>
          <p className="text-sm text-muted-foreground">{t.accountsHint(DEMO_PASSWORD)}</p>
        </div>
        <DemoAccounts />
      </section>
      <div className="flex flex-col gap-3">
        <Link href={`/${DEMO_BUSINESS_SLUG}`} className={buttonVariants({ size: "touch", variant: "outline" })}>
          {t.business}
        </Link>
        <Link href="/demo/emails" className={buttonVariants({ size: "touch", variant: "outline" })}>
          {t.inbox}
        </Link>
      </div>
    </main>
  );
}
