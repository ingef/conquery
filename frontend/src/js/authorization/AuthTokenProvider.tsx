import { createContext, useState } from "react";

import { isIDPEnabled } from "../environment";

import { getStoredAuthToken } from "./helper";

export interface AuthTokenContextValue {
  authToken: string;
  setAuthToken: (token: string) => void;
}

export const AuthTokenContext = createContext<AuthTokenContextValue>({
  authToken: "",
  setAuthToken: () => null,
});

export const useAuthTokenContextValue = (): AuthTokenContextValue => {
  const [authToken, setAuthToken] = useState<string>(
    isIDPEnabled ? "" : getStoredAuthToken() || "",
  );

  return { authToken, setAuthToken };
};

export const AuthTokenContextProvider = AuthTokenContext.Provider;
