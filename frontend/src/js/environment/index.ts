export interface Environment {
  basename: string;
  apiUrl: string;
  isProduction: boolean;
  disableLogin: boolean;
  externalSupportedErrorCodes?: { [k: string]: string };
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
export const getExternalSupportedErrorCodes = () =>
  environment ? environment.externalSupportedErrorCodes || {} : {};
