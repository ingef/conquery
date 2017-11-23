// @flow

import 'babel-polyfill';

import './fixEdge';

import React                      from 'react';
import ReactDOM                   from 'react-dom';
import {
  compose,
  applyMiddleware,
  createStore, combineReducers,
} from 'redux';
import { Provider }               from 'react-redux';
import { useRouterHistory }       from 'react-router';
import createHistory              from 'history/lib/createBrowserHistory';
import { syncHistoryWithStore }   from 'react-router-redux';

import './app/actions'; //  To initialize parameterized actions

import { initializeEnvironment }  from './environment';
import type { Environment }       from './environment';
import createMiddleware           from './middleware';

import AppRouter                  from './app/AppRouter';
import conqueryReducers           from './app/reducers';


require('es6-promise').polyfill();

require('font-awesome-webpack');

// TODO: OG image required?
// require('../../images/og.png');
// Required for isomophic-fetch


function makeStore(initialState: Object, middleware, formReducers: Object, isProduction: Boolean) {
  let enhancer;

  if (!isProduction)
    enhancer = compose(
      middleware,
      // Use the Redux devtools extension, but only in development
      window.devToolsExtension ? window.devToolsExtension() : f => f,
    );
  else
    enhancer = compose(middleware);

  // extend with pluggable formReducers from app
  conqueryReducers.form = combineReducers({...conqueryReducers.form, ...formReducers});

  const reducers = combineReducers(conqueryReducers);

  return createStore(reducers, initialState, enhancer);
}

export default function conquery(environment: Environment, forms: Object, defaultForm: string) {
  initializeEnvironment(environment);

  const initialState = {
    form: {
      availableForms: forms,
      activeForm: defaultForm
    }
  };

  // Redux Router setup
  const browserHistory = useRouterHistory(createHistory)({
    basename: environment.basename
  });

  const middleware = applyMiddleware(...createMiddleware(browserHistory));

  // collect reducers from form extension
  const formReducers =
    Object.assign({}, ...Object.values(forms).map(form => ({[form.type]: form.reducer})));
  const store = makeStore(initialState, middleware, formReducers, environment.isProduction);

  const history = syncHistoryWithStore(browserHistory, store);
  // ---------------------
  // RENDER
  // ---------------------
  ReactDOM.render(
    <Provider store={store}>
      <AppRouter history={history} />
    </Provider>,
    document.getElementById('root')
  );
};
