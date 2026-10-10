import { cn } from "@/lib/utils";

/** Selector nativo: en el celular abre la rueda del sistema, que es lo más cómodo con el dedo. */
export function SelectField({
  label,
  className,
  children,
  ...select
}: React.SelectHTMLAttributes<HTMLSelectElement> & { label: string }) {
  return (
    <label className={cn("flex min-w-0 flex-col gap-1.5 text-sm", className)}>
      <span className="font-medium">{label}</span>
      <select
        className="h-11 min-w-0 rounded-lg border border-input bg-background px-3 text-base outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50"
        {...select}
      >
        {children}
      </select>
    </label>
  );
}
