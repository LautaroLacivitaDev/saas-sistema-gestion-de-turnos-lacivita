"use client";

import { ArrowLeftIcon } from "lucide-react";
import { useState } from "react";

import type { Appointment, BookableItem, BusinessPage, Catalog, Hold, Professional } from "@/lib/api/types";
import { choicesFor, type Selection } from "@/lib/booking";
import { localDate } from "@/lib/format";
import { messages } from "@/messages/es-AR";

import { DetailsStep } from "./details-step";
import { DoneStep } from "./done-step";
import { ServiceStep } from "./service-step";
import { TimeStep } from "./time-step";

type Step = "service" | "time" | "details" | "done";

const STEP_NUMBER: Record<Exclude<Step, "done">, number> = { service: 1, time: 2, details: 3 };

/**
 * Reserva en tres pasos: qué (sucursal y servicio), cuándo (profesional, día y horario) y quién (datos o
 * cuenta). Al elegir el horario queda reservado unos minutos mientras se completan los datos.
 */
export function BookingFlow({
  slug,
  business,
  catalog,
  professionals,
  initial,
}: {
  slug: string;
  business: BusinessPage;
  catalog: Catalog;
  professionals: Professional[];
  initial: Selection;
}) {
  const t = messages.booking;
  const validBranch = business.branches.find((branch) => branch.id === initial.branchId)?.id;
  const [branchId, setBranchId] = useState<string | undefined>(
    validBranch ?? (business.branches.length === 1 ? business.branches[0]!.id : undefined),
  );
  const [item, setItem] = useState<BookableItem | undefined>(initial.item);
  const [barberId, setBarberId] = useState<string | null>(initial.barberId ?? null);
  const [step, setStep] = useState<Step>(() =>
    branchId && initial.item && choicesFor(catalog, professionals, initial.item, branchId).length > 0
      ? "time"
      : "service",
  );
  const [hold, setHold] = useState<Hold | null>(null);
  const [appointment, setAppointment] = useState<Appointment | null>(null);
  const [notice, setNotice] = useState<string | null>(null);

  const branch = business.branches.find((candidate) => candidate.id === branchId);
  const timeZone = branch?.timeZone ?? "America/Argentina/Buenos_Aires";
  const [date, setDate] = useState(() => localDate(new Date(), timeZone));

  function back() {
    setNotice(null);
    if (step === "details") {
      setHold(null);
      setStep("time");
    } else if (step === "time") {
      setStep("service");
    }
  }

  /** El horario se perdió (venció o lo tomó otra persona): se vuelve a elegir. */
  function lostHold(message: string) {
    setHold(null);
    setNotice(message);
    setStep("time");
  }

  if (step === "done" && appointment) {
    return <DoneStep slug={slug} businessName={business.name} appointment={appointment} />;
  }

  const number = STEP_NUMBER[step as Exclude<Step, "done">];

  return (
    <div className="flex flex-col gap-5">
      <div className="flex items-center gap-2">
        {step !== "service" && (
          <button
            type="button"
            onClick={back}
            aria-label={t.back}
            className="-ml-2 flex size-11 items-center justify-center rounded-lg hover:bg-muted"
          >
            <ArrowLeftIcon aria-hidden className="size-5" />
          </button>
        )}
        <div className="flex flex-col">
          <p className="text-sm text-muted-foreground">
            {t.stepOf(number, 3)} · {business.name}
          </p>
          <h1 className="text-xl font-semibold">{t.steps[number - 1]}</h1>
        </div>
      </div>

      {step === "service" && (
        <ServiceStep
          business={business}
          catalog={catalog}
          professionals={professionals}
          branchId={branchId}
          item={item}
          onBranch={(id) => {
            setBranchId(id);
            setBarberId(null);
          }}
          onItem={setItem}
          onContinue={() => setStep("time")}
        />
      )}
      {step === "time" && branchId && item && (
        <TimeStep
          slug={slug}
          branchId={branchId}
          timeZone={timeZone}
          item={item}
          choices={choicesFor(catalog, professionals, item, branchId)}
          barberId={barberId}
          date={date}
          notice={notice}
          onBarber={setBarberId}
          onDate={setDate}
          onHeld={(newHold) => {
            setNotice(null);
            setHold(newHold);
            setStep("details");
          }}
        />
      )}
      {step === "details" && hold && branch && (
        <DetailsStep
          slug={slug}
          hold={hold}
          branchName={branch.name}
          timeZone={timeZone}
          onLost={lostHold}
          onConfirmed={(confirmed) => {
            setAppointment(confirmed);
            setStep("done");
          }}
        />
      )}
    </div>
  );
}
