import { FC } from "react";
import { BrowserRouter as Router, Route, Switch } from "react-router-dom";

import {
  AuthTokenContextProvider,
  useAuthTokenContextValue,
} from "../authorization/AuthTokenProvider";
import KeycloakProvider from "../authorization/KeycloakProvider";
import LoginPage from "../authorization/LoginPage";
import WithAuthToken from "../authorization/WithAuthToken";
import { basename } from "../environment";
import type { TabT } from "../pane/types";

import App from "./App";

interface PropsT {
  rightTabs: TabT[];
}

const ContextProviders: FC = ({ children }) => {
  const authTokenContextValue = useAuthTokenContextValue();

  return (
    <AuthTokenContextProvider value={authTokenContextValue}>
      <KeycloakProvider>{children}</KeycloakProvider>
    </AuthTokenContextProvider>
  );
};

const AppRouter = (props: PropsT) => {
  return (
    <Router basename={basename}>
      <ContextProviders>
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
      </ContextProviders>
    </Router>
  );
};

export default AppRouter;
