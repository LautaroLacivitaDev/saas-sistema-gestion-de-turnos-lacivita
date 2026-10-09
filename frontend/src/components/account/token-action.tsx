"use client";

import { useMutation } from "@tanstack/react-query";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useRef } from "react";

import { Notice } from "@/components/notice";
import { buttonVariants } from "@/components/ui/button";
import { useRefreshAccount } from "@/lib/account";
import { api } from "@/lib/api/client";
import { messages } from "@/messages/es-AR";

/**
 * Usa el token de un link de email apenas se abre la página: verificar el email o ingresar. Se usa una
 * sola vez (los tokens son de un solo uso, y React puede montar dos veces en desarrollo).
 */
export function TokenAction({ kind }: { kind: "verify" | "access" }) {
  const t = messages.account;
  const token = useSearchParams().get("token") ?? "";
  const router = useRouter();
  const refreshAccount = useRefreshAccount();
  const started = useRef(false);

  const { mutate, isError, isSuccess } = useMutation({
    mutationFn: () =>
      api(kind === "verify" ? "/api/auth/email-verification" : "/api/auth/login-link/consume", {
        method: "POST",
        body: { token },
      }),
    onSuccess: async () => {
      await refreshAccount();
      if (kind === "access") {
        router.replace("/mis-turnos");
      }
    },
  });

  useEffect(() => {
    if (token && !started.current) {
      started.current = true;
      mutate();
    }
  }, [token, mutate]);

  if (!token || isError) {
    return <Notice tone="error">{kind === "verify" ? t.verifyFailed : t.accessFailed}</Notice>;
  }
  if (isSuccess && kind === "verify") {
    return (
      <div className="flex flex-col gap-4">
        <Notice tone="success">{t.verified}</Notice>
        <Link href="/mis-turnos" className={buttonVariants({ size: "touch", className: "self-start" })}>
          {t.continue}
        </Link>
      </div>
    );
  }
  return <p className="text-muted-foreground">{kind === "verify" ? t.verifying : t.accessTitle}</p>;
}
