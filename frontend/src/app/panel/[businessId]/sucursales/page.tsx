import { Branches } from "@/components/panel/branches/branches";
import { messages } from "@/messages/es-AR";

export const metadata = { title: messages.panel.branches.title };

// Ver el layout del panel: se arma en el navegador según la sesión.
export const instant = false;

export default function BranchesPage() {
  return <Branches />;
}
