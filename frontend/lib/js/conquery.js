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

import './localization'; // To initialize locales
import './app/actions'; //  To initialize parameterized actions

import { BASENAME, isProduction } from './environment';
import createMiddleware           from './middleware';

import AppRouter                  from './app/AppRouter';
import conqueryReducers           from './app/reducers';


require('es6-promise').polyfill();

require('font-awesome-webpack');
require('../styles/styles.sass');
require('../../app/images/favicon.png'); // TODO

// TODO: OG image required?
// require('../../images/og.png');
// Required for isomophic-fetch


function makeStore(initialState: Object, middleware, formReducers: Object) {
  let enhancer;

  if (!isProduction)
    enhancer = compose(
      middleware,
      // Use the Redux devtools extention, but only in development
      window.devToolsExtension ? window.devToolsExtension() : f => f,
    );
  else
    enhancer = compose(middleware);

  // extend with pluggable formReducers from app
  conqueryReducers.form = combineReducers({...conqueryReducers.form, ...formReducers});

  const reducers = combineReducers(conqueryReducers);

  return createStore(reducers, initialState, enhancer);
}

export default function conquery(forms: Object) {
  const initialState = {};

  // Redux Router setup
  const browserHistory = useRouterHistory(createHistory)({
    basename: BASENAME
  });

  const middleware = applyMiddleware(...createMiddleware(browserHistory));

  // collect reducers from form extension
  const formReducers = Object.values(forms).map(form => form.reducer);
  const store = makeStore(initialState, middleware, formReducers);

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
