import React from "react";
import { BrowserRouter as Router, Route, Switch } from "react-router-dom";
import type { TabT } from "../pane/types";

import LoginPage from "../authorization/LoginPage";
import WithAuthToken from "../authorization/WithAuthToken";

import App from "./App";
import { basename } from "../environment";

interface PropsT {
  rightTabs: TabT[];
}

const AppRouter = (props: PropsT) => {
  return (
    <Router basename={basename()}>
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
  );
};

export default AppRouter;
