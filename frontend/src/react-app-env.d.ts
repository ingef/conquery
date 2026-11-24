/// <reference types="vite/client" />

import type { ConceptIdT, ConceptT } from "./js/api/types";

declare namespace NodeJS {
  interface ProcessEnv {
    NODE_ENV: "development" | "production";
    REACT_APP_API_URL?: string;
    REACT_APP_DISABLE_LOGIN?: boolean;
    REACT_APP_LANG?: "de" | "en";
    PORT?: string;
  }
}

declare global {
  // build-time constants, see `define` in vite.config.ts
  const __BUILD_GIT_DESCRIBE__: string;
  const __BUILD_TIMESTAMP__: string;

  interface Window {
    datasetId: string | null;
    conceptTrees: Record<ConceptIdT, Record<ConceptIdT, ConceptT>>;
    env: Record<string, string>; // To inject env variables at container runtime
  }
}
