import { defineConfig } from "vitest/config";
import react from "@vitejs/plugin-react";

const gatewayUrl = process.env.VITE_GATEWAY_URL ?? "http://localhost:8000";

export default defineConfig({
    plugins: [react()],
    server: {
        port: 5173,
        proxy: {
            "/api": {
                target: gatewayUrl,
                changeOrigin: false
            }
        }
    },
    test: {
        globals: true,
        environment: "jsdom",
        setupFiles: ["./vitest.setup.ts"],
        css: false
    }
});
