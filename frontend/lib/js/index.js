// @flow

import 'babel-polyfill';

import './fixEdge';

import React                           from 'react';
import ReactDOM                        from 'react-dom';
import { AppContainer as HotReloader } from 'react-hot-loader';

import './localization'; // To initialize locales
import './app/actions'; //  To initialize parameterized actions
import {store, browserHistory }        from './store'
import AppRoot                         from "./AppRoot";


require('es6-promise').polyfill();

require('font-awesome-webpack');
require('../styles/styles.sass');
require('../images/favicon.png');

// TODO: OG image required?
// require('../../images/og.png');
// Required for isomophic-fetch

// Render the App including Hot Module Replacement
const renderRoot = () => ReactDOM.render(
  <HotReloader>
    <AppRoot store={store} browserHistory={browserHistory} />
  </HotReloader>,
  document.getElementById('root')
);

export default function conquery() {
  renderRoot();

  // ---Hot Module Replacement
  if (module.hot)
    module.hot.accept('./AppRoot', renderRoot);
};
