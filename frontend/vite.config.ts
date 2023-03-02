import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";
import eslint from "vite-plugin-eslint";

// https://vitejs.dev/config/
export default defineConfig(() => {
  return {
    build: {
      sourcemap: true,
      minify: "terser",
    },
    envPrefix: "REACT_APP_",
    plugins: [
      eslint(),
      react({
        fastRefresh: process.env.NODE_ENV !== "test",
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
  };
});
