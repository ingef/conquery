import react from "@vitejs/plugin-react";
import { defineConfig, loadEnv } from "vite";

// https://vitejs.dev/config/
export default defineConfig(({ command, mode }) => {
  return {
    build: {
      sourcemap: true,
      minify: "terser",
    },
    envPrefix: "REACT_APP_",
    plugins: [
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
    },
  };
});
