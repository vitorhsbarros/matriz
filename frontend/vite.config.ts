/// <reference types="vitest/config" />
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// The build goes into the Spring Boot JAR (target/classes/static), so the browser application and
// the API are served from the same site (ADR-0002). In development, the Vite server forwards the
// API and the health check to Spring Boot on port 8080.
export default defineConfig({
  plugins: [react()],
  build: {
    outDir: "../target/classes/static",
    emptyOutDir: true,
  },
  server: {
    proxy: {
      "/api": "http://localhost:8080",
      "/actuator": "http://localhost:8080",
    },
  },
  test: {
    environment: "jsdom",
  },
});
