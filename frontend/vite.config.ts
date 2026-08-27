/// <reference types="vitest/config" />

import fs from "node:fs";
import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// https://vitejs.dev/config/
export default defineConfig({
  build: {
    sourcemap: true,
    assetsInlineLimit: 0,
  },
  envPrefix: "REACT_APP_",
  plugins: [
    tailwindcss(),
    react({
      jsxImportSource: "@emotion/react",
    }),
  ],
  test: {
    environment: "node",
    include: ["src/**/*.{test,spec}.{ts,tsx}"],
  },
  server: {
    port: 8000,
    open: true,
  },
  define: {
    __BUILD_TIMESTAMP__: JSON.stringify(
      new Date().toISOString().split(".")[0].split("T").join(" "),
    ),
    __BUILD_GIT_DESCRIBE__: JSON.stringify(
      fs.existsSync("./git_describe.txt")
        ? fs.readFileSync("./git_describe.txt", "utf-8").trim()
        : "__BUILD_GIT_DESCRIBE__",
    ),
  },
});
