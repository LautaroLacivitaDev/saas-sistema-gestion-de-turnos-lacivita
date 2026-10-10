import type { Metadata } from "next";
import { Suspense } from "react";

import { AcceptInvitation } from "@/components/account/accept-invitation";
import { messages } from "@/messages/es-AR";

export const metadata: Metadata = {
  title: messages.invitation.title,
  robots: { index: false },
  referrer: "no-referrer",
};

export default function InvitationPage() {
  const t = messages.invitation;
  return (
    <main className="mx-auto flex w-full max-w-md flex-1 flex-col gap-5 px-4 py-8">
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-semibold tracking-tight">{t.title}</h1>
        <p className="text-muted-foreground">{t.text}</p>
      </div>
      <Suspense>
        <AcceptInvitation />
      </Suspense>
    </main>
  );
}
