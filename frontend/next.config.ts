import type { NextConfig } from "next";

// En desarrollo, las llamadas a /api/* se redirigen al backend de Spring Boot.
// Así el navegador ve un solo dominio y la cookie de sesión funciona sin CORS.
// En producción lo resuelve el proxy inverso (ver MEMORY.md).
const backendUrl = process.env.BACKEND_URL ?? "http://localhost:8080";

const nextConfig: NextConfig = {
  cacheComponents: true,
  partialPrefetching: true,
  turbopack: {
    rules: {
      "*.css": {
        loaders: ["@tailwindcss/turbopack"],
        as: "*.css",
      },
    },
  },
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: `${backendUrl}/api/:path*`,
      },
    ];
  },
};

export default nextConfig;
