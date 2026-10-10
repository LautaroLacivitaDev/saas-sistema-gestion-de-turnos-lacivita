"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useEffect } from "react";

import { buttonVariants } from "@/components/ui/button";
import { useAccount } from "@/lib/account";
import { useMemberships } from "@/lib/panel";
import { messages } from "@/messages/es-AR";

const t = messages.panel;

/** Entrada al panel: con un solo negocio entra directo a la agenda; con varios, se elige; sin ninguno, se crea. */
export function PanelHome() {
  const router = useRouter();
  const account = useAccount();
  const memberships = useMemberships();
  const only = memberships.data?.length === 1 ? memberships.data[0] : undefined;

  useEffect(() => {
    if (only) {
      router.replace(`/panel/${only.businessId}/agenda`);
    }
  }, [only, router]);

  if (account.isPending || (account.data && memberships.isPending) || only) {
    return <p className="text-muted-foreground">{t.loading}</p>;
  }
  if (!account.data) {
    return (
      <div className="flex flex-col items-start gap-4">
        <p>{messages.history.signInPrompt}</p>
        <Link href="/ingresar?next=/panel" className={buttonVariants({ size: "touch" })}>
          {messages.account.signIn}
        </Link>
      </div>
    );
  }
  const list = memberships.data ?? [];
  if (list.length === 0) {
    return (
      <div className="flex flex-col items-start gap-4">
        <p className="text-muted-foreground">{t.noBusiness}</p>
        <Link href="/panel/nuevo" className={buttonVariants({ size: "touch" })}>
          {t.createBusiness}
        </Link>
      </div>
    );
  }
  return (
    <div className="flex flex-col gap-4">
      <h2 className="text-lg font-semibold">{t.chooseBusiness}</h2>
      <ul className="flex flex-col gap-2">
        {list.map((membership) => (
          <li key={membership.businessId}>
            <Link
              href={`/panel/${membership.businessId}/agenda`}
              className="flex min-h-14 items-center justify-between gap-3 rounded-xl border px-4 py-3 hover:bg-muted/50"
            >
              <span className="font-medium">{membership.businessName}</span>
              <span className="text-sm text-muted-foreground">{t.roles[membership.role]}</span>
            </Link>
          </li>
        ))}
      </ul>
      <Link href="/panel/nuevo" className={buttonVariants({ variant: "outline", size: "touch", className: "self-start" })}>
        {t.createBusiness}
      </Link>
    </div>
  );
}
