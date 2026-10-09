"use client";

import Script from "next/script";
import { useEffect, useRef, useState } from "react";

import { messages } from "@/messages/es-AR";

declare global {
  interface Window {
    turnstile?: {
      render: (element: HTMLElement, options: Record<string, unknown>) => string;
      remove: (widgetId: string) => void;
    };
  }
}

const siteKey = process.env.NEXT_PUBLIC_TURNSTILE_SITE_KEY;

/** {@code true} si hay que resolver la verificación anti-bots antes de pedir el código. */
export const humanCheckEnabled = Boolean(siteKey);

/** Lo que se manda al backend cuando Turnstile está desactivado (desarrollo y pruebas). */
export const DISABLED_TOKEN = "turnstile-desactivado";

/**
 * Verificación anti-bots de Cloudflare Turnstile. Sin clave del sitio (desarrollo) no se muestra nada: el
 * backend también la tiene desactivada.
 */
export function Turnstile({ onToken }: { onToken: (token: string | null) => void }) {
  const container = useRef<HTMLDivElement>(null);
  const [ready, setReady] = useState(false);

  useEffect(() => {
    if (!siteKey || !ready || !container.current || !window.turnstile) {
      return;
    }
    const widget = window.turnstile.render(container.current, {
      sitekey: siteKey,
      language: "es",
      callback: (token: string) => onToken(token),
      "expired-callback": () => onToken(null),
      "error-callback": () => onToken(null),
    });
    return () => window.turnstile?.remove(widget);
  }, [ready, onToken]);

  if (!siteKey) {
    return null;
  }
  return (
    <>
      <Script
        src="https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit"
        strategy="afterInteractive"
        onReady={() => setReady(true)}
      />
      <div ref={container} aria-label={messages.booking.humanCheck} />
    </>
  );
}
