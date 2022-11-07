import type { Location } from "history";
import { createContext, useCallback, useState } from "react";
import { useLocation } from "react-router";

import { isIDPEnabled } from "../environment";

import { getStoredAuthToken, storeAuthToken } from "./helper";

interface AuthTokenContextValue {
  authToken: string;
  setAuthToken: (token: string) => void;
}

export const AuthTokenContext = createContext<AuthTokenContextValue>({
  authToken: "",
  setAuthToken: () => null,
});

const getInitialAuthToken = (location: Location): string => {
  if (isIDPEnabled) {
    return "";
  }

  // Store the token from the URL if it is present.
  const { search } = location;
  const params = new URLSearchParams(search);
  const accessToken = params.get("access_token");
  if (accessToken) {
    storeAuthToken(accessToken);
  }

  return getStoredAuthToken() || "";
};

export const useAuthTokenContextValue = (): AuthTokenContextValue => {
  const location = useLocation();

  const [authToken, internalSetAuthToken] = useState<string>(
    getInitialAuthToken(location),
  );

  const setAuthToken = useCallback((token: string) => {
    storeAuthToken(token);
    internalSetAuthToken(token);
  }, []);

  return { authToken, setAuthToken };
};

export const AuthTokenContextProvider = AuthTokenContext.Provider;
