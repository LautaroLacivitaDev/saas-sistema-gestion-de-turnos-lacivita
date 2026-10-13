"use client";

import { useQuery } from "@tanstack/react-query";
import { useRouter, useSearchParams } from "next/navigation";
import { useState } from "react";

import { Field } from "@/components/field";
import { Notice } from "@/components/notice";
import { Button } from "@/components/ui/button";
import { api } from "@/lib/api/client";
import { errorMessage } from "@/lib/api/errors";
import { inboxPath } from "@/lib/demo";
import { shortDateTime } from "@/lib/format";
import { messages } from "@/messages/es-AR";

const t = messages.demo;
const TIME_ZONE = "America/Argentina/Buenos_Aires";

type DemoEmail = {
  id: string;
  subject: string;
  text: string;
  html: string | null;
  hasCalendar: boolean;
  createdAt: string;
};

/** Buzón de la demo: los emails que recibió una dirección, del más nuevo al más viejo. */
export function DemoInbox() {
  const router = useRouter();
  const email = useSearchParams().get("email")?.trim() ?? "";
  const [draft, setDraft] = useState(email);
  const inbox = useQuery({
    queryKey: ["demo-inbox", email],
    queryFn: () => api<DemoEmail[]>(`/api/public/demo/inbox?email=${encodeURIComponent(email)}`),
    enabled: email.length > 0,
    // El email que se está esperando puede llegar en unos segundos.
    refetchInterval: 10_000,
  });

  return (
    <div className="flex flex-col gap-4">
      <form
        className="flex flex-col gap-3"
        onSubmit={(event) => {
          event.preventDefault();
          router.replace(inboxPath(draft));
        }}
      >
        <Field
          id="demo-inbox-email"
          label={t.inboxEmail}
          type="email"
          inputMode="email"
          autoComplete="email"
          required
          value={draft}
          onChange={(event) => setDraft(event.target.value)}
        />
        <Button type="submit" size="touch">
          {t.open}
        </Button>
      </form>
      {inbox.isError && <Notice tone="error">{errorMessage(inbox.error)}</Notice>}
      {inbox.isPending && email && <p className="text-muted-foreground">{messages.panel.loading}</p>}
      {inbox.data?.length === 0 && <p className="text-muted-foreground">{t.empty}</p>}
      <ul className="flex flex-col gap-3">
        {inbox.data?.map((message, index) => (
          <li key={message.id}>
            <EmailCard message={message} address={email} open={index === 0} />
          </li>
        ))}
      </ul>
    </div>
  );
}

function EmailCard({ message, address, open }: { message: DemoEmail; address: string; open: boolean }) {
  return (
    <details open={open} className="rounded-xl border">
      <summary className="flex min-h-14 cursor-pointer list-none flex-col justify-center px-4 py-2">
        <span className="font-medium">{message.subject}</span>
        <span className="text-sm text-muted-foreground">{shortDateTime(message.createdAt, TIME_ZONE)}</span>
      </summary>
      <div className="flex flex-col gap-3 border-t p-3">
        {message.html ? (
          <iframe
            title={message.subject}
            // Sin scripts ni acceso al sitio: solo se ve el email. Los links abren en otra pestaña.
            sandbox="allow-popups allow-popups-to-escape-sandbox"
            srcDoc={`<base target="_blank">${message.html}`}
            className="h-[28rem] w-full rounded-lg border bg-white"
          />
        ) : (
          <pre className="font-sans text-sm break-words whitespace-pre-wrap">
            <Linkified text={message.text} />
          </pre>
        )}
        {message.hasCalendar && (
          <a
            href={`/api/public/demo/inbox/${message.id}/turno.ics?email=${encodeURIComponent(address)}`}
            className="flex min-h-11 items-center text-sm font-medium underline underline-offset-4"
          >
            {t.calendar}
          </a>
        )}
      </div>
    </details>
  );
}

/** Los links del texto (verificar email, acceso, invitación) se pueden tocar. */
function Linkified({ text }: { text: string }) {
  return text.split(/(https?:\/\/\S+)/g).map((part, index) =>
    /^https?:\/\//.test(part) ? (
      <a key={index} href={part} className="font-medium break-all underline underline-offset-4">
        {part}
      </a>
    ) : (
      part
    ),
  );
}
