import { redirect } from "next/navigation";
import { Suspense } from "react";

/** El panel de un negocio abre en la agenda. */
export default function BusinessPanelPage({ params }: PageProps<"/panel/[businessId]">) {
  return (
    <Suspense>
      <ToAgenda params={params} />
    </Suspense>
  );
}

async function ToAgenda({ params }: Pick<PageProps<"/panel/[businessId]">, "params">): Promise<never> {
  const { businessId } = await params;
  redirect(`/panel/${businessId}/agenda`);
}
