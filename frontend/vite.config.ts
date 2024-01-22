import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";
import eslint from "vite-plugin-eslint";

// https://vitejs.dev/config/
export default defineConfig({
  build: {
    sourcemap: true,
    minify: "terser",
  },
  envPrefix: "REACT_APP_",
  plugins: [
    eslint(),
    react({
      jsxImportSource: "@emotion/react",
      babel: {
        plugins: ["@emotion/babel-plugin"],
      },
    }),
  ],
  server: {
    port: 8000,
    open: true,
  },
  define: {
    __BUILD_TIMESTAMP__: JSON.stringify(
      new Date().toISOString().split(".")[0].split("T").join(" "),
    ),
  },
});
