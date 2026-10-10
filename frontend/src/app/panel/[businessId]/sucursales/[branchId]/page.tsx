import { Suspense } from "react";

import { BranchDetail } from "@/components/panel/branches/branch-detail";
import { messages } from "@/messages/es-AR";

export const metadata = { title: messages.panel.branches.title };

// Ver el layout del panel: se arma en el navegador según la sesión.
export const instant = false;

export default function BranchPage({ params }: PageProps<"/panel/[businessId]/sucursales/[branchId]">) {
  return (
    <Suspense>
      <Branch params={params} />
    </Suspense>
  );
}

async function Branch({ params }: Pick<PageProps<"/panel/[businessId]/sucursales/[branchId]">, "params">) {
  const { branchId } = await params;
  return <BranchDetail branchId={branchId} />;
}
