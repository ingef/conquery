// @flow

export type Environment = {
  basename: string,
  apiUrl: string,
  isProduction: boolean
};

let environment: ?Environment = null;

export const initializeEnvironment = (env: Environment) => {
  environment = env;
};

export const apiUrl = () => environment ? environment.apiUrl : "";
export const basename = () => environment ? environment.basename : "";
export const isProduction = () => environment ? environment.isProduction : true;
