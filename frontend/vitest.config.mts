import { defineConfig } from "vitest/config";

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
