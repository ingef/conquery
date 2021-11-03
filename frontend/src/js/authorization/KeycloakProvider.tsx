import { ReactKeycloakProvider } from "@react-keycloak/web";
import { FC, useContext } from "react";

import keycloak from "../../keycloak";
import { isIDPEnabled } from "../environment";

import { AuthTokenContext } from "./AuthTokenProvider";

const KeycloakProvider: FC = ({ children }) => {
  const { setAuthToken } = useContext(AuthTokenContext);

  return (
    <ReactKeycloakProvider
      authClient={keycloak}
      onEvent={(event: unknown, error: unknown) => {
        // USEFUL FOR DEBUGGING
        // console.log("onKeycloakEvent", event, error);
      }}
      onTokens={(tokens) => {
        if (tokens.token) {
          setAuthToken(tokens.token);
        }
      }}
      initOptions={{
        pkceMethod: "S256",
        onLoad: isIDPEnabled ? "login-required" : "check-sso",
        // silentCheckSsoRedirectUri:
        //   window.location.origin + "/silent-check-sso.html",
      }}
    >
      {children}
    </ReactKeycloakProvider>
  );
};
export default KeycloakProvider;
