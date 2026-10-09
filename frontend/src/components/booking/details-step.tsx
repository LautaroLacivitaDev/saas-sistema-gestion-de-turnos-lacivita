"use client";

import { zodResolver } from "@hookform/resolvers/zod";
import { useMutation } from "@tanstack/react-query";
import Link from "next/link";
import { usePathname, useSearchParams } from "next/navigation";
import { useCallback, useEffect, useRef, useState } from "react";
import { useForm } from "react-hook-form";
import { z } from "zod";

import { BottomBar, BottomBarSpacer } from "@/components/bottom-bar";
import { Field } from "@/components/field";
import { Notice } from "@/components/notice";
import { Button } from "@/components/ui/button";
import { useAccount } from "@/lib/account";
import { api } from "@/lib/api/client";
import { ApiError, errorMessage } from "@/lib/api/errors";
import type { Account, Appointment, Hold } from "@/lib/api/types";
import { dateTime, money } from "@/lib/format";
import { messages } from "@/messages/es-AR";

import { DISABLED_TOKEN, Turnstile, humanCheckEnabled } from "./turnstile";

const t = messages.booking;

const guestSchema = z.object({
  name: z.string().trim().min(1, messages.errors.required).max(120),
  email: z.string().trim().min(1, messages.errors.required).email(messages.errors.invalidEmail),
  phone: z.string().trim().max(30),
});
type Guest = z.infer<typeof guestSchema>;

const codeSchema = z.object({ code: z.string().trim().regex(/^\d{6}$/, messages.errors.codeFormat) });

/** Errores que significan que el horario ya no está guardado. */
const LOST_HOLD = new Set(["hold_expired", "appointment_not_found", "slot_not_available"]);

/**
 * Paso 3: quién reserva. Con sesión y email verificado alcanza con confirmar; si no, deja sus datos y
 * confirma con el código que le llega por email.
 */
export function DetailsStep({
  slug,
  hold,
  branchName,
  timeZone,
  onLost,
  onConfirmed,
}: {
  slug: string;
  hold: Hold;
  branchName: string;
  timeZone: string;
  onLost: (message: string) => void;
  onConfirmed: (appointment: Appointment) => void;
}) {
  const account = useAccount();
  const remaining = useCountdown(hold.expiresAt, () => onLost(t.expired));
  const base = `/api/public/businesses/${slug}/holds/${hold.holdId}`;

  const confirm = useMutation({
    mutationFn: (code: string | null) =>
      api<Appointment>(`${base}/confirm`, { method: "POST", body: code ? { code } : {} }),
    onSuccess: onConfirmed,
    onError: (error) => {
      if (error instanceof ApiError && LOST_HOLD.has(error.code)) {
        onLost(error.code === "slot_not_available" ? t.taken : t.expired);
      }
    },
  });

  const verified = account.data?.emailVerified === true;

  return (
    <>
      <Summary hold={hold} branchName={branchName} timeZone={timeZone} remaining={remaining} />
      {account.isPending ? null : verified ? (
        <>
          <p className="text-muted-foreground">{t.signedInAs(account.data!.name)}</p>
          {confirm.isError && <Notice tone="error">{errorMessage(confirm.error)}</Notice>}
          <BottomBarSpacer />
          <BottomBar>
            <Button size="touch" className="w-full" disabled={confirm.isPending} onClick={() => confirm.mutate(null)}>
              {confirm.isPending ? t.confirming : t.confirm}
            </Button>
          </BottomBar>
        </>
      ) : (
        <GuestDetails
          base={base}
          account={account.data ?? null}
          confirming={confirm.isPending}
          confirmError={confirm.isError ? confirm.error : null}
          onCode={(code) => confirm.mutate(code)}
          onLost={onLost}
        />
      )}
    </>
  );
}

function Summary({
  hold,
  branchName,
  timeZone,
  remaining,
}: {
  hold: Hold;
  branchName: string;
  timeZone: string;
  remaining: string;
}) {
  return (
    <section aria-labelledby="resumen" className="flex flex-col gap-2 rounded-xl border p-4">
      <h2 id="resumen" className="text-sm font-medium text-muted-foreground">
        {t.summary}
      </h2>
      <p className="font-medium">
        {hold.lines.map((line) => line.serviceName).join(" + ")} {t.with(hold.barberName)}
      </p>
      <p className="first-letter:uppercase">{dateTime(hold.startsAt, timeZone)}</p>
      <p className="text-sm text-muted-foreground">{branchName}</p>
      <p className="flex justify-between border-t pt-2 font-medium">
        <span>{t.total}</span>
        <span>{money(hold.totalPrice)}</span>
      </p>
      <p className="text-sm text-muted-foreground" aria-live="polite">
        {t.held(remaining)}
      </p>
    </section>
  );
}

/** Datos de quien reserva como invitado y el código del email. */
function GuestDetails({
  base,
  account,
  confirming,
  confirmError,
  onCode,
  onLost,
}: {
  base: string;
  account: Account | null;
  confirming: boolean;
  confirmError: Error | null;
  onCode: (code: string) => void;
  onLost: (message: string) => void;
}) {
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const [sentTo, setSentTo] = useState<string | null>(null);
  const [humanToken, setHumanToken] = useState<string | null>(humanCheckEnabled ? null : DISABLED_TOKEN);
  const onToken = useCallback((token: string | null) => setHumanToken(token), []);

  const guest = useForm<Guest>({
    resolver: zodResolver(guestSchema),
    defaultValues: { name: account?.name ?? "", email: account?.email ?? "", phone: "" },
  });
  const codeForm = useForm<{ code: string }>({ resolver: zodResolver(codeSchema), defaultValues: { code: "" } });

  const sendCode = useMutation({
    mutationFn: (data: Guest) =>
      api<void>(`${base}/guest-code`, {
        method: "POST",
        body: { name: data.name, email: data.email, phone: data.phone || undefined, humanToken },
      }),
    onSuccess: (_, data) => setSentTo(data.email),
    onError: (error) => {
      if (error instanceof ApiError) {
        if (LOST_HOLD.has(error.code)) {
          onLost(t.expired);
          return;
        }
        for (const [field, message] of Object.entries(error.fieldErrors)) {
          if (field === "name" || field === "email" || field === "phone") {
            guest.setError(field, { message });
          }
        }
      }
    },
  });

  const next = `${pathname}${searchParams.size ? `?${searchParams}` : ""}`;

  if (sentTo) {
    return (
      <form
        noValidate
        onSubmit={codeForm.handleSubmit((data) => onCode(data.code))}
        className="flex flex-col gap-4"
      >
        <Notice>{t.codeSent(sentTo)}</Notice>
        <Field
          id="code"
          label={t.code}
          inputMode="numeric"
          autoComplete="one-time-code"
          maxLength={6}
          error={
            codeForm.formState.errors.code?.message ??
            (confirmError ? errorMessage(confirmError) : undefined)
          }
          {...codeForm.register("code")}
        />
        <div className="flex flex-wrap gap-2">
          <Button
            type="button"
            variant="outline"
            size="touch"
            disabled={sendCode.isPending}
            onClick={() => sendCode.mutate(guest.getValues())}
          >
            {t.resendCode}
          </Button>
          <Button type="button" variant="ghost" size="touch" onClick={() => setSentTo(null)}>
            {t.changeData}
          </Button>
        </div>
        <BottomBarSpacer />
        <BottomBar>
          <Button type="submit" size="touch" className="w-full" disabled={confirming}>
            {confirming ? t.confirming : t.confirm}
          </Button>
        </BottomBar>
      </form>
    );
  }

  return (
    <form noValidate onSubmit={guest.handleSubmit((data) => sendCode.mutate(data))} className="flex flex-col gap-4">
      {!account && (
        <p className="text-sm text-muted-foreground">
          {t.signInHint}{" "}
          <Link
            href={`/ingresar?next=${encodeURIComponent(next)}`}
            className="inline-flex min-h-11 items-center underline underline-offset-4"
          >
            {t.signIn}
          </Link>
        </p>
      )}
      <Field
        id="name"
        label={t.name}
        autoComplete="name"
        error={guest.formState.errors.name?.message}
        {...guest.register("name")}
      />
      <Field
        id="email"
        label={t.email}
        type="email"
        inputMode="email"
        autoComplete="email"
        error={guest.formState.errors.email?.message}
        {...guest.register("email")}
      />
      <Field
        id="phone"
        label={t.phone}
        type="tel"
        inputMode="tel"
        autoComplete="tel"
        hint={t.phoneHint}
        error={guest.formState.errors.phone?.message}
        {...guest.register("phone")}
      />
      <Turnstile onToken={onToken} />
      {sendCode.isError && !(sendCode.error instanceof ApiError && Object.keys(sendCode.error.fieldErrors).length) && (
        <Notice tone="error">{errorMessage(sendCode.error)}</Notice>
      )}
      <BottomBarSpacer />
      <BottomBar>
        <Button type="submit" size="touch" className="w-full" disabled={sendCode.isPending || !humanToken}>
          {sendCode.isPending ? t.sendingCode : t.sendCode}
        </Button>
      </BottomBar>
    </form>
  );
}

/** Minutos y segundos que quedan del horario guardado ("4:32"). Al llegar a cero, avisa. */
function useCountdown(expiresAt: string, onExpired: () => void): string {
  const deadline = new Date(expiresAt).getTime();
  const [now, setNow] = useState(() => Date.now());
  useEffect(() => {
    const timer = setInterval(() => setNow(Date.now()), 1000);
    return () => clearInterval(timer);
  }, []);
  const left = Math.max(0, deadline - now);
  const expired = useRef(onExpired);
  useEffect(() => {
    expired.current = onExpired;
  });
  useEffect(() => {
    if (left === 0) {
      expired.current();
    }
  }, [left]);
  const seconds = Math.floor(left / 1000);
  return `${Math.floor(seconds / 60)}:${String(seconds % 60).padStart(2, "0")}`;
}
