import type { Metadata } from "next";
import Link from "next/link";
import { Suspense } from "react";

import { PanelHome } from "@/components/panel/panel-home";
import { messages } from "@/messages/es-AR";

export const metadata: Metadata = { title: messages.panel.title, robots: { index: false } };

export default function PanelPage() {
  return (
    <main className="mx-auto flex w-full max-w-xl flex-1 flex-col gap-5 px-4 py-6">
      <Link href="/" className="text-lg font-semibold">
        {messages.app.name}
      </Link>
      <h1 className="text-2xl font-semibold tracking-tight">{messages.panel.title}</h1>
      <Suspense fallback={<p className="text-muted-foreground">{messages.panel.loading}</p>}>
        <PanelHome />
      </Suspense>
    </main>
  );
}
