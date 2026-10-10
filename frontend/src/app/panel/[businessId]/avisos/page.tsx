import { Notices } from "@/components/panel/notices";
import { messages } from "@/messages/es-AR";

export const metadata = { title: messages.panel.notices.title };

// Ver el layout del panel: se arma en el navegador según la sesión.
export const instant = false;

export default function NoticesPage() {
  return <Notices />;
}
