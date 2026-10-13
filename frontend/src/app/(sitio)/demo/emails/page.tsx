import type { Metadata } from "next";
import { notFound } from "next/navigation";
import { Suspense } from "react";

import { DemoInbox } from "@/components/demo/demo-inbox";
import { demoMode } from "@/lib/demo";
import { messages } from "@/messages/es-AR";

export const metadata: Metadata = { title: messages.demo.inbox, robots: { index: false } };

export default function DemoInboxPage() {
  if (!demoMode) {
    notFound();
  }
  const t = messages.demo;
  return (
    <main className="mx-auto flex w-full max-w-xl flex-1 flex-col gap-5 px-4 py-8">
      <div className="flex flex-col gap-2">
        <h1 className="text-2xl font-semibold tracking-tight">{t.inbox}</h1>
        <p className="text-sm text-muted-foreground">{t.inboxHint}</p>
      </div>
      <Suspense>
        <DemoInbox />
      </Suspense>
    </main>
  );
}
