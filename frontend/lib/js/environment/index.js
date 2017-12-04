// @flow

export type Environment = {
  basename: String,
  apiUrl: String,
  isProduction: Boolean
};

let environment: Environment = null;

export const initializeEnvironment = (env: Environment) => {
  environment = env;
};

export const apiUrl = () => environment.apiUrl;
