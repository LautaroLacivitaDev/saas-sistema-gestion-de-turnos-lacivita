import { useId } from "react";

import { cn } from "@/lib/utils";

/**
 * Selector nativo: en el celular abre la rueda del sistema, que es lo más cómodo con el dedo. Como
 * {@link Field}, muestra una ayuda o el error debajo, asociados al selector para los lectores de pantalla.
 */
export function SelectField({
  label,
  hint,
  error,
  className,
  children,
  ...select
}: React.SelectHTMLAttributes<HTMLSelectElement> & { label: string; hint?: string; error?: string }) {
  const id = useId();
  const describedBy = [error ? `${id}-error` : null, hint && !error ? `${id}-hint` : null].filter(Boolean).join(" ");
  return (
    <div className={cn("flex min-w-0 flex-col gap-1.5 text-sm", className)}>
      <label htmlFor={id} className="font-medium">
        {label}
      </label>
      <select
        id={id}
        aria-invalid={error ? true : undefined}
        aria-describedby={describedBy || undefined}
        className="h-11 min-w-0 rounded-lg border border-input bg-background px-3 text-base outline-none focus-visible:border-ring focus-visible:ring-3 focus-visible:ring-ring/50 disabled:opacity-60 aria-invalid:border-destructive"
        {...select}
      >
        {children}
      </select>
      {hint && !error && (
        <span id={`${id}-hint`} className="text-muted-foreground">
          {hint}
        </span>
      )}
      {error && (
        <span id={`${id}-error`} className="text-destructive">
          {error}
        </span>
      )}
    </div>
  );
}
