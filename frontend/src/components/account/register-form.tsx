"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { Field } from "@/components/field";
import { Notice } from "@/components/notice";
import { Button } from "@/components/ui/button";
import { useRefreshAccount } from "@/lib/account";
import { api } from "@/lib/api/client";
import { ApiError, errorMessage } from "@/lib/api/errors";
import { messages } from "@/messages/es-AR";

import { safeNext } from "./safe-next";

const t = messages.account;

const schema = z.object({
  name: z.string().trim().min(1, messages.errors.required).max(120),
  email: z.string().trim().min(1, messages.errors.required).email(messages.errors.invalidEmail),
  password: z.string().min(8, messages.errors.passwordLength).max(64, messages.errors.passwordLength),
});
type Register = z.infer<typeof schema>;

/** Crear una cuenta con email y contraseña. Le llega un email para verificar la dirección. */
export function RegisterForm() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const next = safeNext(searchParams.get("next"));
  const refreshAccount = useRefreshAccount();
  const form = useForm<Register>({
    resolver: zodResolver(schema),
    defaultValues: { name: "", email: "", password: "" },
  });

  const register = useMutation({
    mutationFn: (data: Register) => api("/api/auth/register", { method: "POST", body: data }),
    onSuccess: async () => {
      await refreshAccount();
      router.replace(next);
    },
    onError: (error) => {
      if (error instanceof ApiError) {
        for (const [field, message] of Object.entries(error.fieldErrors)) {
          if (field === "name" || field === "email" || field === "password") {
            form.setError(field, { message });
          }
        }
      }
    },
  });
  const showGeneralError =
    register.isError &&
    !(register.error instanceof ApiError && Object.keys(register.error.fieldErrors).length > 0);

  return (
    <form noValidate className="flex flex-col gap-4" onSubmit={form.handleSubmit((data) => register.mutate(data))}>
      <Field
        id="name"
        label={t.name}
        autoComplete="name"
        error={form.formState.errors.name?.message}
        {...form.register("name")}
      />
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
        autoComplete="new-password"
        hint={t.passwordHint}
        error={form.formState.errors.password?.message}
        {...form.register("password")}
      />
      {showGeneralError && <Notice tone="error">{errorMessage(register.error)}</Notice>}
      <Button type="submit" size="touch" disabled={register.isPending}>
        {register.isPending ? t.registering : t.register}
      </Button>
      <p className="text-center text-sm text-muted-foreground">
        {t.haveAccount}{" "}
        <Link
          href={`/ingresar?next=${encodeURIComponent(next)}`}
          className="inline-flex min-h-11 items-center underline underline-offset-4"
        >
          {t.signIn}
        </Link>
      </p>
    </form>
  );
}
