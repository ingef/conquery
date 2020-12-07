import React, { FC } from "react";
import { Provider } from "react-redux";
import { hot } from "react-hot-loader";
import { StateT } from "app-types";

import type { TabT } from "./pane/types";

import AppRouter from "./app/AppRouter";
import { Store } from "redux";

interface PropsT {
  store: Store<StateT>;
  browserHistory: Object;
  rightTabs: TabT[];
}

const AppRoot: FC<PropsT> = ({ store, browserHistory, rightTabs }) => (
  <Provider store={store}>
    <AppRouter history={browserHistory} rightTabs={rightTabs} />
  </Provider>
);

export default hot(module)(AppRoot);
