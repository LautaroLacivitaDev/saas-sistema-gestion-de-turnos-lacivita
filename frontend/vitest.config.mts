import { defineConfig } from "vitest/config";

// DECISIÓN: sin @vitejs/plugin-react. Su versión actual choca con Babel 7 que trae Next,
// y Vite 8 ya compila JSX/TSX y resuelve los alias de tsconfig por su cuenta.

export default defineConfig({
  resolve: {
    // Resuelve el alias "@/..." definido en tsconfig.json.
    tsconfigPaths: true,
  },
  test: {
    environment: "jsdom",
    setupFiles: ["./vitest.setup.ts"],
    include: ["src/**/*.test.{ts,tsx}"],
  },
});
