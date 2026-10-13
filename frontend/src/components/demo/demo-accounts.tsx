"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useRouter } from "next/navigation";

import { Notice } from "@/components/notice";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import { DEMO_ACCOUNTS, DEMO_PASSWORD } from "@/lib/demo";
import { messages } from "@/messages/es-AR";

const t = messages.demo;

/** Entrar con un toque como alguien del negocio de ejemplo. */
export function DemoAccounts() {
  const router = useRouter();
  const client = useQueryClient();
  const signIn = useMutation({
    mutationFn: (account: (typeof DEMO_ACCOUNTS)[number]) =>
      api("/api/auth/login", { method: "POST", body: { email: account.email, password: DEMO_PASSWORD } }),
    onSuccess: (_, account) => {
      // Lo que estaba en memoria era de otra persona.
      client.clear();
      router.push(account.next);
    },
  });
  return (
    <div className="flex flex-col gap-3">
      <ul className="flex flex-col gap-2">
        {DEMO_ACCOUNTS.map((account) => (
          <li key={account.email} className="flex items-center gap-3 rounded-xl border px-4 py-3">
            <span className="flex min-w-0 flex-1 flex-col">
              <span className="font-medium">{t.roles[account.role]}</span>
              <span className="truncate text-sm text-muted-foreground">{account.email}</span>
            </span>
            <Button
              size="touch"
              className="shrink-0"
              disabled={signIn.isPending}
              onClick={() => signIn.mutate(account)}
            >
              {signIn.isPending && signIn.variables?.email === account.email ? t.entering : t.enter}
            </Button>
          </li>
        ))}
      </ul>
      {signIn.isError && <Notice tone="error">{errorMessage(signIn.error)}</Notice>}
    </div>
  );
}
