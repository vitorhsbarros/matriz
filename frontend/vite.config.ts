/// <reference types="vitest/config" />
import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";

// The build goes into the Spring Boot JAR (target/classes/static), so the browser application and
// the API are served from the same site (ADR-0002). In development, the Vite server forwards the
// API and the health check to Spring Boot, on port 8080 unless MATRIZ_API_PORT says otherwise.
export default defineConfig(({ mode }) => {
  const apiPort = loadEnv(mode, ".", "MATRIZ_API_PORT").MATRIZ_API_PORT || "8080";
  const api = `http://localhost:${apiPort}`;
  return {
    plugins: [react()],
    build: {
      outDir: "../target/classes/static",
      emptyOutDir: true,
    },
    server: {
      proxy: {
        "/api": api,
        "/actuator": api,
      },
    },
    test: {
      environment: "jsdom",
    },
  };
});
