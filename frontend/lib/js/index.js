// @flow

import './fixEdge';

import React                           from 'react';
import ReactDOM                        from 'react-dom';
import createHistory                   from 'history/createBrowserHistory';

import './app/actions'; //  To initialize parameterized actions
import { makeStore }                   from './store'
import AppRoot                         from './AppRoot';

import {
  initializeEnvironment,
  type Environment,
  basename,
}                                      from './environment';

require('es6-promise').polyfill();

require('font-awesome-webpack');

// TODO: OG image required?
// require('../../images/og.png');
// Required for isomophic-fetch

let store;
let browserHistory;
const initialState = {};

// Render the App including Hot Module Replacement
const renderRoot = (tabs: Object) => {
  browserHistory = browserHistory || createHistory({
    basename: basename()
  });
  store = store || makeStore(initialState, browserHistory, tabs);

  ReactDOM.render(
    <AppRoot store={store} browserHistory={browserHistory} tabs={tabs} />,
    document.getElementById('root')
  );
};

export default function conquery(environment: Environment, tabs: Object) {
  initializeEnvironment(environment);
  renderRoot(tabs);
};
