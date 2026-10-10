"use client";

import { useMutation, useQueryClient } from "@tanstack/react-query";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";

import { Notice } from "@/components/notice";
import { Button, buttonVariants } from "@/components/ui/button";
import { useAccount } from "@/lib/account";
import { api } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import type { Membership } from "@/lib/api/types";
import { messages } from "@/messages/es-AR";

const t = messages.invitation;

/**
 * Aceptar una invitación al equipo. Hace falta una cuenta con el mismo email: quien no ingresó, ingresa o
 * se registra y vuelve acá. Se acepta con un botón (no al abrir el link) para que sea una decisión explícita.
 */
export function AcceptInvitation() {
  const token = useSearchParams().get("token") ?? "";
  const account = useAccount();
  const router = useRouter();
  const client = useQueryClient();
  const accept = useMutation({
    mutationFn: () => api<Membership>("/api/invitations/accept", { method: "POST", body: { token } }),
    onSuccess: async (membership) => {
      await client.invalidateQueries({ queryKey: ["memberships"] });
      router.replace(`/panel/${membership.businessId}`);
    },
  });

  if (!token) {
    return <Notice tone="error">{t.missing}</Notice>;
  }
  if (account.isPending) {
    return <p className="text-muted-foreground">{messages.panel.loading}</p>;
  }
  if (!account.data) {
    const next = encodeURIComponent(`/invitacion?token=${token}`);
    return (
      <div className="flex flex-col gap-3">
        <Link href={`/ingresar?next=${next}`} className={buttonVariants({ size: "touch" })}>
          {t.signIn}
        </Link>
        <Link href={`/registro?next=${next}`} className={buttonVariants({ size: "touch", variant: "outline" })}>
          {t.register}
        </Link>
      </div>
    );
  }
  return (
    <div className="flex flex-col gap-4">
      <p className="text-sm text-muted-foreground">{t.signedInAs(account.data.email)}</p>
      {accept.isError && <Notice tone="error">{errorMessage(accept.error)}</Notice>}
      <Button size="touch" disabled={accept.isPending || accept.isSuccess} onClick={() => accept.mutate()}>
        {accept.isPending ? t.accepting : t.accept}
      </Button>
    </div>
  );
}
