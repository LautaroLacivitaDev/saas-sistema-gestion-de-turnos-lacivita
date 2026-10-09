import { cn } from "@/lib/utils";

/** Mensaje destacado: un error, una confirmación o una aclaración. Los lectores de pantalla lo anuncian. */
export function Notice({
  tone = "info",
  children,
  className,
}: {
  tone?: "info" | "success" | "error";
  children: React.ReactNode;
  className?: string;
}) {
  return (
    <p
      role={tone === "error" ? "alert" : "status"}
      className={cn(
        "rounded-lg px-3 py-2 text-sm",
        tone === "info" && "bg-muted text-foreground",
        tone === "success" && "bg-emerald-50 text-emerald-900 dark:bg-emerald-950 dark:text-emerald-100",
        tone === "error" && "bg-destructive/10 text-destructive",
        className,
      )}
    >
      {children}
    </p>
  );
}
