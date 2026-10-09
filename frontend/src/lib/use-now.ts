"use client";

import { useState } from "react";

/** La hora al mostrar la pantalla. Fija mientras la pantalla está abierta: el render queda puro. */
export function useNow(): number {
  const [now] = useState(Date.now);
  return now;
}
