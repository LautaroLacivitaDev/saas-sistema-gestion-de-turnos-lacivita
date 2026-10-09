"use client";

import { CheckIcon, CopyIcon } from "lucide-react";
import { useState } from "react";

import { Button } from "@/components/ui/button";
import { messages } from "@/messages/es-AR";

/** Copia el link al portapapeles. En celulares que lo permiten, abre el menú de compartir del sistema. */
export function CopyLinkButton({ url, title }: { url: string; title: string }) {
  const [copied, setCopied] = useState(false);

  async function share() {
    if (navigator.share) {
      try {
        await navigator.share({ title, url });
        return;
      } catch {
        // La persona cerró el menú o el navegador no pudo: se copia el link.
      }
    }
    await navigator.clipboard.writeText(url);
    setCopied(true);
    setTimeout(() => setCopied(false), 2500);
  }

  return (
    <Button type="button" variant="outline" size="touch" onClick={share}>
      {copied ? <CheckIcon aria-hidden /> : <CopyIcon aria-hidden />}
      <span aria-live="polite">{copied ? messages.business.copied : messages.business.copyLink}</span>
    </Button>
  );
}
