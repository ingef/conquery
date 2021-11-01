import { createContext, useCallback, useState } from "react";

import { isIDPEnabled } from "../environment";

import { getStoredAuthToken, storeAuthToken } from "./helper";

export interface AuthTokenContextValue {
  authToken: string;
  setAuthToken: (token: string) => void;
}

export const AuthTokenContext = createContext<AuthTokenContextValue>({
  authToken: "",
  setAuthToken: () => null,
});

export const useAuthTokenContextValue = (): AuthTokenContextValue => {
  const [authToken, internalSetAuthToken] = useState<string>(
    isIDPEnabled ? "" : getStoredAuthToken() || "",
  );

  const setAuthToken = useCallback((token: string) => {
    storeAuthToken(token);
    internalSetAuthToken(token);
  }, []);

  return { authToken, setAuthToken };
};

export const AuthTokenContextProvider = AuthTokenContext.Provider;
