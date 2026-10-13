import Link from "next/link";

import { demoMode } from "@/lib/demo";
import { messages } from "@/messages/es-AR";

/** Franja que avisa que el sitio es una demo y lleva a las cuentas de prueba y al buzón. */
export function DemoBanner() {
  if (!demoMode) {
    return null;
  }
  return (
    <p className="bg-amber-100 px-4 py-2 text-center text-sm text-amber-950 dark:bg-amber-950 dark:text-amber-100">
      {messages.demo.banner}{" "}
      <Link href="/demo" className="font-medium underline underline-offset-4">
        {messages.demo.bannerLink}
      </Link>
    </p>
  );
}
