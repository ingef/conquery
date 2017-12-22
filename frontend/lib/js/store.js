import {applyMiddleware, compose, createStore}   from "redux";
import createHistory                             from 'history/createBrowserHistory';

import reducers                                  from "./app/reducers";
import {BASENAME, isProduction}                  from "./environment";
import createMiddleware                          from "./middleware";

function makeStore(initialState: Object, middleware) {
  let enhancer;

  if (!isProduction)
    enhancer = compose(
      middleware,
      // Use the Redux devtools extention, but only in development
      window.devToolsExtension ? window.devToolsExtension() : f => f,
    );
  else
    enhancer = compose(middleware);

  return createStore(reducers, initialState, enhancer);
}

const initialState = {};

// Redux Router setup
export const browserHistory = createHistory({
  basename: BASENAME
});

const middleware = applyMiddleware(...createMiddleware(browserHistory));

export const store = makeStore(initialState, middleware);

