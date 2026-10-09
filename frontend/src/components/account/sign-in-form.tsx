"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { Field } from "@/components/field";
import { Notice } from "@/components/notice";
import { Button } from "@/components/ui/button";
import { useRefreshAccount } from "@/lib/account";
import { api } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import { messages } from "@/messages/es-AR";

import { safeNext } from "./safe-next";

const t = messages.account;

const emailSchema = z.object({
  email: z.string().trim().min(1, messages.errors.required).email(messages.errors.invalidEmail),
});
const schema = emailSchema.extend({ password: z.string().min(1, messages.errors.required) });
type SignIn = z.infer<typeof schema>;

/** Ingresar con email y contraseña, o pedir un link de acceso por email. */
export function SignInForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const next = safeNext(searchParams.get("next"));
  const refreshAccount = useRefreshAccount();
  const [linkMode, setLinkMode] = useState(false);

  const form = useForm<SignIn>({ resolver: zodResolver(schema), defaultValues: { email: "", password: "" } });
  const linkForm = useForm<{ email: string }>({ resolver: zodResolver(emailSchema), defaultValues: { email: "" } });

  const signIn = useMutation({
    mutationFn: (data: SignIn) => api("/api/auth/login", { method: "POST", body: data }),
    onSuccess: async () => {
      await refreshAccount();
      router.replace(next);
    },
  });
  const sendLink = useMutation({
    mutationFn: (email: string) => api("/api/auth/login-link", { method: "POST", body: { email } }),
  });

  if (linkMode) {
    return (
      <form
        noValidate
        className="flex flex-col gap-4"
        onSubmit={linkForm.handleSubmit((data) => sendLink.mutate(data.email))}
      >
        <Field
          id="link-email"
          label={t.email}
          type="email"
          inputMode="email"
          autoComplete="email"
          error={linkForm.formState.errors.email?.message}
          {...linkForm.register("email")}
        />
        {sendLink.isSuccess && <Notice tone="success">{t.linkSent}</Notice>}
        {sendLink.isError && <Notice tone="error">{errorMessage(sendLink.error)}</Notice>}
        <Button type="submit" size="touch" disabled={sendLink.isPending}>
          {t.sendLink}
        </Button>
        <Button type="button" variant="ghost" size="touch" onClick={() => setLinkMode(false)}>
          {t.signIn}
        </Button>
      </form>
    );
  }

  return (
    <form noValidate className="flex flex-col gap-4" onSubmit={form.handleSubmit((data) => signIn.mutate(data))}>
      <Field
        id="email"
        label={t.email}
        type="email"
        inputMode="email"
        autoComplete="email"
        error={form.formState.errors.email?.message}
        {...form.register("email")}
      />
      <Field
        id="password"
        label={t.password}
        type="password"
        autoComplete="current-password"
        error={form.formState.errors.password?.message}
        {...form.register("password")}
      />
      {signIn.isError && <Notice tone="error">{errorMessage(signIn.error)}</Notice>}
      <Button type="submit" size="touch" disabled={signIn.isPending}>
        {signIn.isPending ? t.signingIn : t.signIn}
      </Button>
      <Button type="button" variant="ghost" size="touch" onClick={() => setLinkMode(true)}>
        {t.linkInstead}
      </Button>
      <p className="text-center text-sm text-muted-foreground">
        {t.noAccount}{" "}
        <Link
          href={`/registro?next=${encodeURIComponent(next)}`}
          className="inline-flex min-h-11 items-center underline underline-offset-4"
        >
          {t.createAccount}
        </Link>
      </p>
    </form>
  );
}
