export type Environment = {
  basename: string;
  apiUrl: string;
  isProduction: boolean;
  disableLogin: boolean;
};

let environment: Environment | null = null;

export const initializeEnvironment = (env: Environment) => {
  environment = env;
};

export const apiUrl = () => (environment ? environment.apiUrl : "");
export const basename = () => (environment ? environment.basename : "");
export const isProduction = () =>
  environment ? environment.isProduction : true;
export const isLoginDisabled = () => !!environment && environment.disableLogin;
