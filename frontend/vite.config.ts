/// <reference types="vitest/config" />

import { execSync } from "node:child_process";
import fs from "node:fs";
import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// the docker build has no .git and writes the file in an earlier stage
function gitDescribe() {
  if (fs.existsSync("./git_describe.txt")) {
    return fs.readFileSync("./git_describe.txt", "utf-8").trim();
  }
  try {
    return execSync("git describe --tags --always", { encoding: "utf-8" })
      .trim()
      .replace(/^v/, "");
  } catch {
    return "unknown";
  }
}

// https://vitejs.dev/config/
export default defineConfig({
  build: {
    sourcemap: true,
    assetsInlineLimit: 0,
  },
  envPrefix: "REACT_APP_",
  plugins: [tailwindcss(), react()],
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
    __BUILD_GIT_DESCRIBE__: JSON.stringify(gitDescribe()),
  },
});
