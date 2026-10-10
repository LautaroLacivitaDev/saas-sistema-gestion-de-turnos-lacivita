import type { Metadata } from "next";
import { Suspense } from "react";

import { TokenAction } from "@/components/account/token-action";
import { messages } from "@/messages/es-AR";

export const metadata: Metadata = {
  title: messages.account.accessTitle,
  robots: { index: false },
  referrer: "no-referrer",
};

export default function AccessPage() {
  return (
    <main className="mx-auto flex w-full max-w-md flex-1 flex-col gap-5 px-4 py-8">
      <h1 className="text-2xl font-semibold tracking-tight">{messages.account.accessTitle}</h1>
      <Suspense>
        <TokenAction kind="access" />
      </Suspense>
    </main>
  );
}
