import type { Metadata } from "next";
import { Suspense } from "react";

import { ManageAppointment } from "@/components/manage/manage-appointment";
import { messages } from "@/messages/es-AR";

export const metadata: Metadata = {
  title: messages.manage.title,
  robots: { index: false },
  // El token del link va en la URL: no se lo pasa a otros sitios en el encabezado Referer.
  referrer: "no-referrer",
};

export default function ManagePage() {
  return (
    <main className="mx-auto flex w-full max-w-3xl flex-1 flex-col gap-5 px-4 py-6">
      <h1 className="text-2xl font-semibold tracking-tight">{messages.manage.title}</h1>
      <Suspense fallback={<p className="text-muted-foreground">{messages.manage.loading}</p>}>
        <ManageAppointment />
      </Suspense>
    </main>
  );
}
