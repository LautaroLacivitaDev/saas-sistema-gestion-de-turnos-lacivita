import { RoleGate } from "@/components/panel/role-gate";
import { CustomerList } from "@/components/panel/customers/customer-list";
import { messages } from "@/messages/es-AR";

export const metadata = { title: messages.panel.customers.title };

// Ver el layout del panel: se arma en el navegador según la sesión.
export const instant = false;

export default function CustomersPage() {
  return (
    <RoleGate role="MANAGER">
      <CustomerList />
    </RoleGate>
  );
}
