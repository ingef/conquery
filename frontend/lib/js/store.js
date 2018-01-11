import {applyMiddleware, compose, createStore}   from "redux";
import createHistory                             from 'history/createBrowserHistory';

import buildAppReducer                           from './app/reducers';
import {BASENAME, isProduction}                  from "./environment";
import createMiddleware                          from "./middleware";

// Redux Router setup
export const browserHistory = createHistory({
  basename: BASENAME
});

const middleware = applyMiddleware(...createMiddleware(browserHistory));

export function makeStore(initialState: Object, forms: Object) {
  let enhancer;

  if (!isProduction)
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
