import QRCode from "qrcode";

/**
 * Código QR de un link, como SVG armado en el servidor (no suma JavaScript al celular). Sirve para que el
 * negocio lo imprima en el mostrador y los clientes reserven escaneándolo.
 */
export async function QrCode({ value, label, size = 160 }: { value: string; label: string; size?: number }) {
  const svg = await QRCode.toString(value, { type: "svg", margin: 1, errorCorrectionLevel: "M" });
  return (
    <div
      role="img"
      aria-label={label}
      className="rounded-lg bg-white p-2 [&_svg]:h-full [&_svg]:w-full"
      style={{ width: size, height: size }}
      // El SVG lo genera la librería a partir del link: no contiene texto de usuarios.
      dangerouslySetInnerHTML={{ __html: svg }}
    />
  );
}
