import { applyMiddleware, compose, createStore, Store } from "redux";
import multi from "redux-multi";
import thunk from "redux-thunk";

import buildAppReducer from "./app/reducers";
import { isProduction } from "./environment";
import { TabT } from "./pane/types";

export function makeStore(initialState: Object, tabs: Object) {
  const middleware = applyMiddleware(thunk, multi);

  let enhancer;

  if (!isProduction) {
    enhancer = compose(
      middleware,
      // Use the Redux devtools extention, but only in development
      window.devToolsExtension ? window.devToolsExtension() : (f) => f,
    );
  } else {
    enhancer = compose(middleware);
  }

  const store = createStore(buildAppReducer(tabs), initialState, enhancer);

  if (module.hot) {
    // Enable Webpack hot module replacement for reducers
    module.hot.accept("./app/reducers", () => {
      const nextRootReducer = buildAppReducer(tabs);

      store.replaceReducer(nextRootReducer);
    });
  }

  return store;
}

export function updateReducers(store: Store, tabs: TabT[]) {
  store.replaceReducer(buildAppReducer(tabs));
}
