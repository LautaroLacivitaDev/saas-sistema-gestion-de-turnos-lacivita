import type { Metadata, Viewport } from "next";
import { Geist, Geist_Mono } from "next/font/google";

import { siteUrl } from "@/lib/site";
import { messages } from "@/messages/es-AR";

import { Providers } from "./providers";
import "./globals.css";

const geistSans = Geist({
  variable: "--font-geist-sans",
  subsets: ["latin"],
});

const geistMono = Geist_Mono({
  variable: "--font-geist-mono",
  subsets: ["latin"],
});

export const metadata: Metadata = {
  metadataBase: new URL(siteUrl),
  title: { default: messages.app.name, template: `%s · ${messages.app.name}` },
  description: messages.app.description,
  openGraph: { siteName: messages.app.name, locale: "es_AR", type: "website" },
};

// Mobile first: la mayoría entra desde el celular. No se bloquea el zoom (accesibilidad) y
// "cover" deja usar toda la pantalla; el contenido respeta las zonas seguras con env(safe-area-inset-*).
export const viewport: Viewport = {
  width: "device-width",
  initialScale: 1,
  viewportFit: "cover",
};

export default function RootLayout({ children }: LayoutProps<"/">) {
  return (
    <html
      lang="es-AR"
      className={`${geistSans.variable} ${geistMono.variable} h-full antialiased`}
    >
      <body className="flex min-h-dvh flex-col pt-[env(safe-area-inset-top)] pb-[env(safe-area-inset-bottom)] pl-[env(safe-area-inset-left)] pr-[env(safe-area-inset-right)]">
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
