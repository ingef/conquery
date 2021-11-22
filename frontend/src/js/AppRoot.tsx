import { StateT } from "app-types";
import { FC } from "react";
import { Provider } from "react-redux";
import { Store } from "redux";

import AppRouter from "./app/AppRouter";
import type { TabT } from "./pane/types";

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
