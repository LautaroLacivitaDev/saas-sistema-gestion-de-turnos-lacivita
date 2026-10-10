import type { Metadata } from "next";
import { Suspense } from "react";

import { MyAppointments } from "@/components/account/my-appointments";
import { messages } from "@/messages/es-AR";

export const metadata: Metadata = { title: messages.history.title, robots: { index: false } };

export default function MyAppointmentsPage() {
  return (
    <main className="mx-auto flex w-full max-w-3xl flex-1 flex-col gap-5 px-4 py-6">
      <h1 className="text-2xl font-semibold tracking-tight">{messages.history.title}</h1>
      <Suspense fallback={<p className="text-muted-foreground">{messages.history.loading}</p>}>
        <MyAppointments />
      </Suspense>
    </main>
  );
}
