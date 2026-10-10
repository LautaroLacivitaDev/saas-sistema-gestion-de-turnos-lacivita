import { Schedules } from "@/components/panel/schedule/schedules";
import { messages } from "@/messages/es-AR";

export const metadata = { title: messages.panel.schedules.title };

// Ver el layout del panel: se arma en el navegador según la sesión.
export const instant = false;

export default function SchedulesPage() {
  return <Schedules />;
}
