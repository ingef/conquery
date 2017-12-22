// @flow

import 'babel-polyfill';

import './fixEdge';

import React                      from 'react';
import ReactDOM                   from 'react-dom';
import { Provider }               from 'react-redux';

import './localization'; // To initialize locales
import './app/actions'; //  To initialize parameterized actions
import {store, browserHistory }   from './store'
import AppRouter                  from './app/AppRouter';


require('es6-promise').polyfill();

require('font-awesome-webpack');
require('../styles/styles.sass');
require('../images/favicon.png');

// TODO: OG image required?
// require('../../images/og.png');
// Required for isomophic-fetch

export default function conquery() {
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
