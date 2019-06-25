// @flow

import React from "react";
import { Route, Switch, Router } from "react-router";
import type { TabType } from "../pane/types";

import { Unauthorized, WithAuthToken } from "../authorization";

import App from "./App";

type PropsType = {
  history: Object,
  rightTabs: TabType[]
};

const AppWithAuthToken = WithAuthToken(App);

const AppRouter = ({ history, ...rest }: PropsType) => {
  return (
    <Router history={history}>
      <Switch>
        <Route path="/unauthorized" component={Unauthorized} />
        <Route
          path="/*"
          render={routeProps => <AppWithAuthToken {...routeProps} {...rest} />}
        />
      </Switch>
    </Router>
  );
};

export default AppRouter;
