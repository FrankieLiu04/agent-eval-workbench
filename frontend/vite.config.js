import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  build: {
    emptyOutDir: true,
    outDir: "../target/classes/static",
  },
  server: {
    proxy: {
      "/api": process.env.VITE_PROXY_TARGET || "http://127.0.0.1:8080",
      "/actuator": process.env.VITE_PROXY_TARGET || "http://127.0.0.1:8080",
    },
  },
});
