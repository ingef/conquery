import react from "@vitejs/plugin-react";
import fs from "fs";
import { defineConfig } from "vite";
import eslint from "vite-plugin-eslint";

// https://vitejs.dev/config/
export default defineConfig({
  build: {
    sourcemap: true,
    minify: "terser",
    assetsInlineLimit: 0,
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
    __BUILD_GIT_DESCRIBE__: JSON.stringify(
      fs.existsSync("./git_describe.txt")
        ? fs.readFileSync("./git_describe.txt", "utf-8").trim()
        : "__BUILD_GIT_DESCRIBE__",
    ),
  },
});
