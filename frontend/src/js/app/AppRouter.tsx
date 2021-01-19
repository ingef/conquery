import React from "react";
import { Route, Switch, Router } from "react-router";
import { ReactKeycloakProvider } from "@react-keycloak/web";
import keycloak from "../../keycloak";
import type { TabT } from "../pane/types";

import LoginPage from "../authorization/LoginPage";
import WithAuthToken from "../authorization/WithAuthToken";

import App from "./App";

type PropsType = {
  history: Object;
  rightTabs: TabT[];
};

const AppRouter = ({ history, ...rest }: PropsType) => {
  return (
    <ReactKeycloakProvider
      authClient={keycloak}
      initOptions={{
        onLoad: "check-sso",
        pkceMethod: "S256",
      }}
    >
      <Router history={history}>
        <Switch>
          <Route path="/login" component={LoginPage} />
          <Route
            path="/*"
            render={(routeProps) => (
              <WithAuthToken {...routeProps}>
                <App {...rest} />
              </WithAuthToken>
            )}
          />
        </Switch>
      </Router>
    </ReactKeycloakProvider>
  );
};

export default AppRouter;
