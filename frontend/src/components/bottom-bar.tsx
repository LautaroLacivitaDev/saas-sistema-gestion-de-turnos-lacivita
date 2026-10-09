import { cn } from "@/lib/utils";

/**
 * Barra fija al pie con la acción principal, al alcance del pulgar. Respeta la zona segura de los
 * celulares con barra de gestos. La página deja lugar al final para que la barra no tape contenido.
 */
export function BottomBar({ children, className }: { children: React.ReactNode; className?: string }) {
  return (
    <div className="fixed inset-x-0 bottom-0 z-20 border-t bg-background/95 pb-[env(safe-area-inset-bottom)] backdrop-blur supports-[backdrop-filter]:bg-background/80">
      <div className={cn("mx-auto flex max-w-3xl items-center gap-3 px-4 py-3", className)}>{children}</div>
    </div>
  );
}

/** Espacio al final de la página para que la barra fija no tape lo último. */
export function BottomBarSpacer() {
  return <div aria-hidden className="h-24" />;
}
