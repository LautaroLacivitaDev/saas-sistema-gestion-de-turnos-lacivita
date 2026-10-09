import Image from "next/image";

import { initials } from "@/lib/format";
import { cn } from "@/lib/utils";

/**
 * Foto de un profesional o, si no tiene, sus iniciales. La foto es un link que carga el profesional (de
 * cualquier sitio https), por eso no pasa por la optimización de imágenes de Next.
 */
export function Avatar({
  name,
  photoUrl,
  size = 48,
  className,
}: {
  name: string;
  photoUrl: string | null;
  size?: number;
  className?: string;
}) {
  if (photoUrl) {
    return (
      <Image
        src={photoUrl}
        alt=""
        width={size}
        height={size}
        unoptimized
        className={cn("shrink-0 rounded-full object-cover", className)}
        style={{ width: size, height: size }}
      />
    );
  }
  return (
    <span
      aria-hidden
      className={cn(
        "flex shrink-0 items-center justify-center rounded-full bg-muted font-medium text-muted-foreground",
        className,
      )}
      style={{ width: size, height: size, fontSize: size / 2.6 }}
    >
      {initials(name)}
    </span>
  );
}
