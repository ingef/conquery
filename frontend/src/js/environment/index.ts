import { TFunction } from "react-i18next";

// See index.html for an inject marker, that we use to inject env vars
// at caontainer runtime
function runtimeVar(variable: string): string | null {
  return window.env && window.env[variable] !== "null"
    ? window.env[variable]
    : null;
}

// Needs to be explicit because weback statically replaces process.env.XXX through DefinePlugin
const isProductionEnv = runtimeVar("NODE_ENV") || process.env.NODE_ENV;
const languageEnv = runtimeVar("REACT_APP_LANG") || process.env.REACT_APP_LANG;
const apiUrlEnv =
  runtimeVar("REACT_APP_API_URL") || process.env.REACT_APP_API_URL;
const disableLoginEnv =
  runtimeVar("REACT_APP_DISABLE_LOGIN") || process.env.REACT_APP_DISABLE_LOGIN;
const enableIDPEnv =
  runtimeVar("REACT_APP_IDP_ENABLE") || process.env.REACT_APP_IDP_ENABLE;
const basenameEnv =
  runtimeVar("REACT_APP_BASENAME") || process.env.REACT_APP_BASENAME;
const idpUrlEnv =
  runtimeVar("REACT_APP_IDP_URL") || process.env.REACT_APP_IDP_URL;
const idpRealmEnv =
  runtimeVar("REACT_APP_IDP_REALM") || process.env.REACT_APP_IDP_REALM;
const idpClientIdEnv =
  runtimeVar("REACT_APP_IDP_CLIENT_ID") || process.env.REACT_APP_IDP_CLIENT_ID;

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
