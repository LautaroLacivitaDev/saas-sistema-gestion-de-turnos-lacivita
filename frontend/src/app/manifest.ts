import type { MetadataRoute } from "next";

import { messages } from "@/messages/es-AR";

/**
 * La app se puede instalar en el celular (PWA). Abre en el panel, que es lo que más usan los equipos;
 * quien no tiene un negocio cae en la entrada del panel y desde ahí sigue.
 */
export default function manifest(): MetadataRoute.Manifest {
  return {
    name: messages.app.name,
    short_name: messages.app.name,
    description: messages.app.description,
    lang: "es-AR",
    start_url: "/panel",
    scope: "/",
    display: "standalone",
    background_color: "#ffffff",
    theme_color: "#0a0a0a",
    icons: [
      { src: "/icon-192.png", sizes: "192x192", type: "image/png" },
      { src: "/icon-512.png", sizes: "512x512", type: "image/png" },
      { src: "/icon-maskable-512.png", sizes: "512x512", type: "image/png", purpose: "maskable" },
    ],
  };
}
