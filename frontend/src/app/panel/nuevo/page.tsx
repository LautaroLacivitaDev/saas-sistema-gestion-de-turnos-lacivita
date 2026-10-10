import type { Metadata } from "next";
import Link from "next/link";

import { Onboarding } from "@/components/panel/onboarding/onboarding";
import { messages } from "@/messages/es-AR";

export const metadata: Metadata = { title: messages.panel.onboarding.title, robots: { index: false } };

export default function NewBusinessPage() {
  const t = messages.panel.onboarding;
  return (
    <main className="mx-auto flex w-full max-w-xl flex-1 flex-col gap-5 px-4 py-6">
      <Link href="/panel" className="text-lg font-semibold">
        {messages.app.name}
      </Link>
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-semibold tracking-tight">{t.title}</h1>
        <p className="text-muted-foreground">{t.intro}</p>
      </div>
      <Onboarding />
    </main>
  );
}
