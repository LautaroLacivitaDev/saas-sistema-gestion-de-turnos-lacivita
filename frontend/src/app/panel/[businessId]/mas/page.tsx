import { MoreMenu } from "@/components/panel/more-menu";
import { messages } from "@/messages/es-AR";

export const metadata = { title: messages.panel.nav.more };

export default function MorePage() {
  return (
    <div className="flex flex-col gap-4">
      <h1 className="text-2xl font-semibold tracking-tight">{messages.panel.nav.more}</h1>
      <MoreMenu />
    </div>
  );
}
