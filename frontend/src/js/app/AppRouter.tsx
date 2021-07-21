import { ReactKeycloakProvider } from "@react-keycloak/web";
import React from "react";
import { BrowserRouter as Router, Route, Switch } from "react-router-dom";

import keycloak from "../../keycloak";
import LoginPage from "../authorization/LoginPage";
import WithAuthToken from "../authorization/WithAuthToken";
import { basename, isIDPEnabled } from "../environment";
import type { TabT } from "../pane/types";

import App from "./App";

interface PropsT {
  rightTabs: TabT[];
}

const AppRouter = (props: PropsT) => {
  return (
    <ReactKeycloakProvider
      authClient={keycloak}
      onEvent={(event: unknown, error: unknown) => {
        // USEFUL FOR DEBUGGING
        // console.log("onKeycloakEvent", event, error);
      }}
      onTokens={(tokens) => {
        // USEFUL FOR DEBUGGING
        // console.log("TOKENS ", tokens);
      }}
      initOptions={{
        pkceMethod: "S256",
        onLoad: isIDPEnabled ? "login-required" : "check-sso",
        // silentCheckSsoRedirectUri:
        //   window.location.origin + "/silent-check-sso.html",
      }}
    >
      <Router basename={basename}>
        <Switch>
          <Route path="/login" component={LoginPage} />
          <Route
            path="/*"
            render={(routeProps) => (
              <WithAuthToken {...routeProps}>
                <App {...props} />
              </WithAuthToken>
            )}
          />
        </Switch>
      </Router>
    </ReactKeycloakProvider>
  );
};

export default AppRouter;
