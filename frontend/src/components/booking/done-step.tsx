import { CheckCircle2Icon } from "lucide-react";
import Link from "next/link";

import { buttonVariants } from "@/components/ui/button";
import type { Appointment } from "@/lib/api/types";
import { dateTime, money } from "@/lib/format";
import { messages } from "@/messages/es-AR";

/** El turno quedó confirmado. */
export function DoneStep({
  slug,
  businessName,
  appointment,
}: {
  slug: string;
  businessName: string;
  appointment: Appointment;
}) {
  const t = messages.booking;
  return (
    <div className="flex flex-col items-center gap-4 py-6 text-center">
      <CheckCircle2Icon aria-hidden className="size-14 text-emerald-600" />
      <h1 className="text-2xl font-semibold text-balance">{t.doneTitle}</h1>
      <div className="flex w-full flex-col gap-1 rounded-xl border p-4 text-left">
        <p className="font-medium">
          {appointment.lines.map((line) => line.serviceName).join(" + ")} {t.with(appointment.barberName)}
        </p>
        <p className="first-letter:uppercase">{dateTime(appointment.startsAt, appointment.timeZone)}</p>
        <p className="text-sm text-muted-foreground">
          {businessName} · {appointment.branchName}
        </p>
        <p className="mt-1 font-medium">{money(appointment.totalPrice)}</p>
      </div>
      <p className="text-pretty text-muted-foreground">{t.doneText}</p>
      <div className="flex w-full flex-col gap-2 sm:flex-row sm:justify-center">
        <Link href={`/${slug}`} className={buttonVariants({ variant: "outline", size: "touch" })}>
          {t.backToBusiness}
        </Link>
        <Link href="/mis-turnos" className={buttonVariants({ size: "touch" })}>
          {t.myAppointments}
        </Link>
      </div>
    </div>
  );
}
