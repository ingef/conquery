// @flow

import React from "react";
import { Provider } from "react-redux";
import { hot } from "react-hot-loader";

import type { TabType } from "./pane/types";

import AppRouter from "./app/AppRouter";

type PropsType = {
  store: Object,
  browserHistory: Object,
  rightTabs: TabType[]
};

const AppRoot = ({ store, browserHistory, rightTabs }: PropsType) => (
  <Provider store={store}>
    <AppRouter history={browserHistory} rightTabs={rightTabs} />
  </Provider>
);

export default hot(module)(AppRoot);
