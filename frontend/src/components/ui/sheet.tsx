"use client";

import { Dialog } from "@base-ui/react/dialog";
import { XIcon } from "lucide-react";

import { messages } from "@/messages/es-AR";

/**
 * Panel que se abre sobre la pantalla. En el celular sube desde abajo (al alcance del pulgar y con scroll
 * si no entra); desde tablet se muestra centrado. Lo maneja quien lo usa con `open` y `onOpenChange`.
 */
export function Sheet({
  open,
  onOpenChange,
  title,
  children,
}: {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  title: string;
  children: React.ReactNode;
}) {
  return (
    <Dialog.Root open={open} onOpenChange={(next) => onOpenChange(next)}>
      <Dialog.Portal>
        <Dialog.Backdrop className="fixed inset-0 z-40 bg-black/40 transition-opacity duration-150 data-ending-style:opacity-0 data-starting-style:opacity-0" />
        <Dialog.Popup className="fixed inset-x-0 bottom-0 z-50 flex max-h-[90dvh] flex-col rounded-t-2xl bg-background pb-[env(safe-area-inset-bottom)] shadow-xl transition-transform duration-200 data-ending-style:translate-y-full data-starting-style:translate-y-full md:inset-x-auto md:top-1/2 md:bottom-auto md:left-1/2 md:w-[32rem] md:-translate-x-1/2 md:-translate-y-1/2 md:rounded-2xl md:data-ending-style:translate-y-[-45%] md:data-starting-style:translate-y-[-45%]">
          <div className="flex items-center justify-between gap-2 border-b px-4 py-2">
            <Dialog.Title className="text-lg font-semibold">{title}</Dialog.Title>
            <Dialog.Close
              aria-label={messages.panel.close}
              className="-mr-2 flex size-11 items-center justify-center rounded-lg hover:bg-muted"
            >
              <XIcon aria-hidden className="size-5" />
            </Dialog.Close>
          </div>
          <div className="overflow-y-auto px-4 py-4">{children}</div>
        </Dialog.Popup>
      </Dialog.Portal>
    </Dialog.Root>
  );
}
