import { Agenda } from "@/components/panel/agenda/agenda";
import { messages } from "@/messages/es-AR";

export const metadata = { title: messages.panel.agenda.title };

// Ver el layout del panel: se arma en el navegador según la sesión.
export const instant = false;

export default function AgendaPage() {
  return <Agenda />;
}
