import { demoMode, inboxPath } from "@/lib/demo";
import { messages } from "@/messages/es-AR";

/**
 * En la demo, donde la app dice "te mandamos un email", el atajo para verlo. Abre en otra pestaña para no
 * perder lo que se estaba haciendo (por ejemplo, el código de la reserva).
 */
export function DemoInboxLink({ email }: { email: string }) {
  if (!demoMode || !email) {
    return null;
  }
  return (
    <a
      href={inboxPath(email)}
      target="_blank"
      rel="noopener"
      className="flex min-h-11 items-center text-sm font-medium underline underline-offset-4"
    >
      {messages.demo.seeInInbox}
    </a>
  );
}
