// @flow

import 'babel-polyfill';

import './fixEdge';

import React                      from 'react';
import ReactDOM                   from 'react-dom';
import {
  compose,
  applyMiddleware,
  createStore,
}                                 from 'redux';
import { Provider }               from 'react-redux';
import createHistory              from 'history/createBrowserHistory';

import './app/actions'; //  To initialize parameterized actions

import {
  initializeEnvironment,
  type Environment,
}                                 from './environment';
import createMiddleware           from './middleware';

import AppRouter                  from './app/AppRouter';
import buildAppReducer            from './app/reducers';


require('es6-promise').polyfill();

require('font-awesome-webpack');

// TODO: OG image required?
// require('../../images/og.png');
// Required for isomophic-fetch


function makeStore(initialState: Object, middleware, forms: Object, isProduction: Boolean) {
  let enhancer;

  if (!isProduction)
    enhancer = compose(
      middleware,
      // Use the Redux devtools extension, but only in development
      window.devToolsExtension ? window.devToolsExtension() : f => f,
    );
  else
    enhancer = compose(middleware);

  return createStore(buildAppReducer(forms), initialState, enhancer);
}

export default function conquery(environment: Environment, forms: Object) {
  initializeEnvironment(environment);

  const initialState = {};

  // Redux Router setup
  const browserHistory = createHistory({
    basename: environment.basename
  });

  const middleware = applyMiddleware(...createMiddleware(browserHistory));

  const store = makeStore(initialState, middleware, forms, environment.isProduction);

  // ---------------------
  // RENDER
  // ---------------------
  ReactDOM.render(
    <Provider store={store}>
      <AppRouter history={browserHistory} />
    </Provider>,
    document.getElementById('root')
  );
};
