import { Provider } from "react-redux";
import { Store } from "redux";

import AppRouter from "./app/AppRouter";
import type { StateT } from "./app/reducers";
import type { TabT } from "./pane/types";

interface Props {
  store: Store<StateT>;
  rightTabs: TabT[];
}

const AppRoot = ({ store, rightTabs }: Props) => (
  <Provider store={store}>
    <AppRouter rightTabs={rightTabs} />
  </Provider>
);

export default AppRoot;
