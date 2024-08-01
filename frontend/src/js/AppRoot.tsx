import { Provider } from "react-redux";
import { Store } from "redux";

import AppRouter from "./app/AppRouter";
import type { StateT } from "./app/reducers";

interface Props {
  store: Store<StateT>;
}

const AppRoot = ({ store }: Props) => (
  <Provider store={store}>
    <AppRouter />
  </Provider>
);

export default AppRoot;
