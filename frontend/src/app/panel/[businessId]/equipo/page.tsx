import { Team } from "@/components/panel/team/team";
import { messages } from "@/messages/es-AR";

export const metadata = { title: messages.panel.team.title };

// Ver el layout del panel: se arma en el navegador según la sesión.
export const instant = false;

export default function TeamPage() {
  return <Team />;
}
