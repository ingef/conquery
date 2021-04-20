import { TFunction } from "react-i18next";

export interface Environment {
  basename: string;
  apiUrl: string;
  isProduction: boolean;
  disableLogin: boolean;
  enableIDP: boolean;
  getExternalSupportedErrorMessage?: (
    t: TFunction,
    code: string,
    context?: Record<string, string>,
  ) => string | undefined;
}

let environment: Environment | null = null;

export const initializeEnvironment = (env: Environment) => {
  environment = env;
};

export const apiUrl = () => (environment ? environment.apiUrl : "");
export const basename = () => (environment ? environment.basename : "");
export const isProduction = () =>
  environment ? environment.isProduction : true;
export const isLoginDisabled = () => !!environment && environment.disableLogin;
export const isIDPEnabled = () => !!environment && environment.enableIDP;
export const getExternalSupportedErrorMessage = (
  t: TFunction,
  code: string,
  context?: Record<string, string>,
) =>
  environment && environment.getExternalSupportedErrorMessage
    ? environment.getExternalSupportedErrorMessage(t, code, context)
    : undefined;
