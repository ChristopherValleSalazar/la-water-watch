import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    // Listen only on this computer. localhost:3000 and 127.0.0.1:3000 open the same page.
    host: "127.0.0.1",
    port: 3000,
    // Fail if port 3000 is busy instead of silently choosing a different port.
    strictPort: true,
  },
});
