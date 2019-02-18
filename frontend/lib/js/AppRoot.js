// @flow

import React                           from 'react';
import { Provider }                    from 'react-redux';
import { hot }                         from 'react-hot-loader'

import AppRouter                       from './app/AppRouter';

type PropsType = {
  store: Object,
  browserHistory: Object
};

const AppRoot = ({ store, browserHistory }: PropsType) => (
  <Provider store={store}>
    <AppRouter history={browserHistory} />
  </Provider>
);

export default hot(module)(AppRoot);
