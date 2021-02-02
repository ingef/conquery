import React, { FC } from "react";
import { Provider } from "react-redux";
import { StateT } from "app-types";

import type { TabT } from "./pane/types";

import AppRouter from "./app/AppRouter";
import { Store } from "redux";

interface PropsT {
  store: Store<StateT>;
  rightTabs: TabT[];
}

const AppRoot: FC<PropsT> = ({ store, rightTabs }) => (
  <Provider store={store}>
    <AppRouter rightTabs={rightTabs} />
  </Provider>
);

export default AppRoot;
