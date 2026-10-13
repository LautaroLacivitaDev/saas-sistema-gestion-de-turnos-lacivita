// Genera los íconos de la PWA en public/ (node scripts/generate-icons.mjs). Se versionan los PNG: el
// script solo hace falta si cambia el diseño.
import { writeFile } from "node:fs/promises";

import { ImageResponse } from "next/og.js";

const BACKGROUND = "#0a0a0a";

/** Una "L" blanca sobre fondo oscuro. "maskable" deja margen para que Android la recorte en círculo. */
function icon(size, { maskable = false, rounded = false } = {}) {
  const letter = Math.round(size * (maskable ? 0.5 : 0.62));
  return new ImageResponse(
    {
      type: "div",
      props: {
        style: {
          width: "100%",
          height: "100%",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          background: BACKGROUND,
          borderRadius: rounded ? size * 0.22 : 0,
          color: "#ffffff",
          fontSize: letter,
          fontWeight: 700,
          letterSpacing: "-0.04em",
        },
        children: "L",
      },
    },
    { width: size, height: size },
  );
}

const outputs = [
  ["public/icon-192.png", icon(192, { rounded: true })],
  ["public/icon-512.png", icon(512, { rounded: true })],
  ["public/icon-maskable-512.png", icon(512, { maskable: true })],
  ["public/apple-touch-icon.png", icon(180)],
];

for (const [path, response] of outputs) {
  await writeFile(path, Buffer.from(await response.arrayBuffer()));
  console.log(path);
}
