import { RoleGate } from "@/components/panel/role-gate";
import { Settings } from "@/components/panel/settings/settings";
import { messages } from "@/messages/es-AR";

export const metadata = { title: messages.panel.settings.title };

// Ver el layout del panel: se arma en el navegador según la sesión.
export const instant = false;

export default function SettingsPage() {
  return (
    <RoleGate role="OWNER">
      <Settings />
    </RoleGate>
  );
}
