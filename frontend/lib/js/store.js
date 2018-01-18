import {applyMiddleware, compose, createStore}   from "redux";

import buildAppReducer                           from './app/reducers';
import { isProduction }                          from "./environment";
import createMiddleware                          from "./middleware";

export function makeStore(initialState: Object, browserHistory: Object, forms: Object) {
  const middleware = applyMiddleware(...createMiddleware(browserHistory));

  let enhancer;

  if (!isProduction())
    enhancer = compose(
      middleware,
      // Use the Redux devtools extention, but only in development
      window.devToolsExtension ? window.devToolsExtension() : f => f,
    );
  else
    enhancer = compose(middleware);

  const store = createStore(buildAppReducer(forms), initialState, enhancer);

  if (module.hot)
    // Enable Webpack hot module replacement for reducers
    module.hot.accept('./app/reducers', () => {
      const nextRootReducer = buildAppReducer(forms);
      store.replaceReducer(nextRootReducer);
    });


  return store;
}
