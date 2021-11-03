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

const AppRouter = (props: PropsT) => {
  const authTokenContextValue = useAuthTokenContextValue();

  return (
    <AuthTokenContextProvider value={authTokenContextValue}>
      <KeycloakProvider>
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
      </KeycloakProvider>
    </AuthTokenContextProvider>
  );
};

export default AppRouter;
