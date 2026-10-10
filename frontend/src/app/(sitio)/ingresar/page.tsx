import type { Metadata } from "next";
import { Suspense } from "react";

import { SignInForm } from "@/components/account/sign-in-form";
import { messages } from "@/messages/es-AR";

export const metadata: Metadata = { title: messages.account.signInTitle, robots: { index: false } };

export default function SignInPage() {
  const t = messages.account;
  return (
    <main className="mx-auto flex w-full max-w-md flex-1 flex-col gap-5 px-4 py-8">
      <div className="flex flex-col gap-1">
        <h1 className="text-2xl font-semibold tracking-tight">{t.signInTitle}</h1>
        <p className="text-muted-foreground">{t.signInText}</p>
      </div>
      <Suspense>
        <SignInForm />
      </Suspense>
    </main>
  );
}
