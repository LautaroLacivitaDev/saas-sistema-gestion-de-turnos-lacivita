"use client";

import { useSyncExternalStore } from "react";

/**
 * {@code true} si la pantalla cumple la media query. En el servidor (sin pantalla) se asume el celular:
 * mobile first.
 */
export function useMedia(query: string): boolean {
  return useSyncExternalStore(
    (onChange) => {
      const media = window.matchMedia(query);
      media.addEventListener("change", onChange);
      return () => media.removeEventListener("change", onChange);
    },
    () => window.matchMedia(query).matches,
    () => false,
  );
}

/** Desde tablet (768 px, el "md" de Tailwind). */
export const TABLET_UP = "(min-width: 768px)";
