"use client";

import { useEffect, useState } from "react";

/** El valor, pero recién cuando deja de cambiar un rato: un buscador no pide datos por cada letra. */
export function useDebounced<T>(value: T, delayMs = 300): T {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delayMs);
    return () => clearTimeout(timer);
  }, [value, delayMs]);
  return debounced;
}
