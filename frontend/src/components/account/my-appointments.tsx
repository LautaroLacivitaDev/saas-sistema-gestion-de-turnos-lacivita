"use client";

import { useMutation, useQuery } from "@tanstack/react-query";
import Link from "next/link";
import { useRouter } from "next/navigation";

import { Notice } from "@/components/notice";
import { Button, buttonVariants } from "@/components/ui/button";
import { useAccount, useRefreshAccount } from "@/lib/account";
import { api } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import type { AccountAppointment } from "@/lib/api/types";
import { repeatHref } from "@/lib/booking";
import { useNow } from "@/lib/use-now";
import { dateTime, money } from "@/lib/format";
import { cn } from "@/lib/utils";
import { messages } from "@/messages/es-AR";

const t = messages.history;

/** Próximo: todavía va a ocurrir (a confirmar o confirmado) y no pasó la hora. */
function isUpcoming(entry: AccountAppointment, now: number): boolean {
  const { status, startsAt } = entry.appointment;
  return (status === "PENDING" || status === "CONFIRMED") && new Date(startsAt).getTime() > now;
}

/** Los turnos de la cuenta en todos los negocios, con "repetir" para volver a reservar lo mismo. */
export function MyAppointments() {
  const account = useAccount();
  const router = useRouter();
  const refreshAccount = useRefreshAccount();
  const now = useNow();
  const history = useQuery({
    queryKey: ["my-appointments"],
    queryFn: () => api<AccountAppointment[]>("/api/me/appointments"),
    enabled: Boolean(account.data),
  });
  const signOut = useMutation({
    mutationFn: () => api("/api/auth/logout", { method: "POST" }),
    onSuccess: async () => {
      await refreshAccount();
      router.replace("/");
    },
  });

  if (account.isPending) {
    return <p className="text-muted-foreground">{t.loading}</p>;
  }
  if (!account.data) {
    return (
      <div className="flex flex-col items-start gap-4">
        <p className="text-muted-foreground">{t.signInPrompt}</p>
        <Link href="/ingresar?next=/mis-turnos" className={buttonVariants({ size: "touch" })}>
          {messages.account.signIn}
        </Link>
      </div>
    );
  }

  const header = (
    <div className="flex items-center justify-between gap-3">
      <p className="text-muted-foreground">{messages.account.hello(account.data.name)}</p>
      <Button variant="ghost" size="touch" disabled={signOut.isPending} onClick={() => signOut.mutate()}>
        {messages.account.signOut}
      </Button>
    </div>
  );

  if (history.isPending) {
    return (
      <>
        {header}
        <p className="text-muted-foreground">{t.loading}</p>
      </>
    );
  }
  if (history.isError) {
    return (
      <>
        {header}
        <Notice tone="error">{errorMessage(history.error)}</Notice>
      </>
    );
  }

  const upcoming = history.data.filter((entry) => isUpcoming(entry, now)).reverse();
  const past = history.data.filter((entry) => !isUpcoming(entry, now));
  const last = history.data[0];

  return (
    <>
      {header}
      {last ? (
        <Link href={repeatHref(last)} className={buttonVariants({ size: "touch" })}>
          {t.repeatLast}
        </Link>
      ) : (
        <div className="flex flex-col items-start gap-3">
          <p className="text-muted-foreground">{t.empty}</p>
          <Link href="/buscar" className={buttonVariants({ size: "touch" })}>
            {messages.home.browseAll}
          </Link>
        </div>
      )}
      {last && (
        <>
          <section className="flex flex-col gap-3" aria-labelledby="proximos">
            <h2 id="proximos" className="text-lg font-semibold">
              {t.upcoming}
            </h2>
            {upcoming.length === 0 ? (
              <p className="text-muted-foreground">{t.noUpcoming}</p>
            ) : (
              <AppointmentList entries={upcoming} />
            )}
            {upcoming.length > 0 && <p className="text-sm text-muted-foreground">{t.manageHint}</p>}
          </section>
          {past.length > 0 && (
            <section className="flex flex-col gap-3" aria-labelledby="anteriores">
              <h2 id="anteriores" className="text-lg font-semibold">
                {t.past}
              </h2>
              <AppointmentList entries={past} />
            </section>
          )}
        </>
      )}
    </>
  );
}

function AppointmentList({ entries }: { entries: AccountAppointment[] }) {
  return (
    <ul className="flex flex-col gap-3">
      {entries.map((entry) => {
        const { appointment } = entry;
        return (
          <li key={appointment.id} className="flex flex-col gap-2 rounded-xl border p-4">
            <div className="flex items-start justify-between gap-3">
              <Link href={`/${entry.slug}`} className="font-medium underline-offset-4 hover:underline">
                {entry.businessName}
              </Link>
              <span
                className={cn(
                  "shrink-0 rounded-full px-2.5 py-0.5 text-sm",
                  appointment.status === "CANCELLED" ? "bg-destructive/10 text-destructive" : "bg-muted",
                )}
              >
                {messages.manage.status[appointment.status] ?? appointment.status}
              </span>
            </div>
            <p>
              {appointment.lines.map((line) => line.serviceName).join(" + ")}{" "}
              {messages.booking.with(appointment.barberName)}
            </p>
            <p className="text-sm text-muted-foreground first-letter:uppercase">
              {dateTime(appointment.startsAt, appointment.timeZone)} · {appointment.branchName} ·{" "}
              {money(appointment.totalPrice)}
            </p>
            <Link
              href={repeatHref(entry)}
              className={buttonVariants({ variant: "outline", size: "touch", className: "self-start" })}
            >
              {t.repeat}
            </Link>
          </li>
        );
      })}
    </ul>
  );
}
