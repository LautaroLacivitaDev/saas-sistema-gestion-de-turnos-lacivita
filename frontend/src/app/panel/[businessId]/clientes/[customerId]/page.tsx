import { Suspense } from "react";

import { CustomerDetail } from "@/components/panel/customers/customer-detail";
import { messages } from "@/messages/es-AR";

export const metadata = { title: messages.panel.customers.title };

// Ver el layout del panel: se arma en el navegador según la sesión.
export const instant = false;

export default function CustomerPage({ params }: PageProps<"/panel/[businessId]/clientes/[customerId]">) {
  return (
    <Suspense>
      <Customer params={params} />
    </Suspense>
  );
}

async function Customer({ params }: Pick<PageProps<"/panel/[businessId]/clientes/[customerId]">, "params">) {
  const { customerId } = await params;
  return <CustomerDetail customerId={customerId} />;
}
