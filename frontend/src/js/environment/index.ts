import { TFunction } from "react-i18next";

// See index.html for an inject marker, that we use to inject env vars
// at container runtime
function runtimeVar(variable: string): string | null {
  return window.env && window.env[variable] !== "null"
    ? window.env[variable]
    : null;
}

// Needs to be explicit import.meta.env.XXX
const isProductionEnv = runtimeVar("NODE_ENV") || import.meta.env.NODE_ENV;
const languageEnv =
  runtimeVar("REACT_APP_LANG") || import.meta.env.REACT_APP_LANG;
const apiUrlEnv =
  runtimeVar("REACT_APP_API_URL") || import.meta.env.REACT_APP_API_URL;
const disableLoginEnv =
  runtimeVar("REACT_APP_DISABLE_LOGIN") ||
  import.meta.env.REACT_APP_DISABLE_LOGIN;
const enableIDPEnv =
  runtimeVar("REACT_APP_IDP_ENABLE") || import.meta.env.REACT_APP_IDP_ENABLE;
const basenameEnv =
  runtimeVar("REACT_APP_BASENAME") || import.meta.env.REACT_APP_BASENAME;
const idpUrlEnv =
  runtimeVar("REACT_APP_IDP_URL") || import.meta.env.REACT_APP_IDP_URL;
const idpRealmEnv =
  runtimeVar("REACT_APP_IDP_REALM") || import.meta.env.REACT_APP_IDP_REALM;
const idpClientIdEnv =
  runtimeVar("REACT_APP_IDP_CLIENT_ID") ||
  import.meta.env.REACT_APP_IDP_CLIENT_ID;

export const isProduction = isProductionEnv === "production" || true;
export const language = languageEnv === "de" ? "de" : "en";
export const apiUrl = apiUrlEnv || "";
export const isLoginDisabled = disableLoginEnv === "true";
export const isIDPEnabled = enableIDPEnv === "true";
export const basename = basenameEnv || "";
export const idpUrl = idpUrlEnv || "";
export const idpRealm = idpRealmEnv || "";
export const idpClientId = idpClientIdEnv || "";

export interface CustomEnvironment {
  getExternalSupportedErrorMessage?: (
    t: TFunction,
    code: string,
    context?: Record<string, string>,
  ) => string | undefined;
}

let customEnvironment: CustomEnvironment | null = null;

export const initializeEnvironment = (env: CustomEnvironment) => {
  customEnvironment = env;
};

export const getExternalSupportedErrorMessage = (
  t: TFunction,
  code: string,
  context?: Record<string, string>,
) =>
  customEnvironment && customEnvironment.getExternalSupportedErrorMessage
    ? customEnvironment.getExternalSupportedErrorMessage(t, code, context)
    : undefined;
