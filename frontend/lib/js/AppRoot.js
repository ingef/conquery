// @flow
import React                           from 'react';
import { Provider }                    from 'react-redux';

import AppRouter                       from './app/AppRouter';

const AppRoot = ({store, browserHistory}) => (
  <Provider store={store}>
    <AppRouter history={browserHistory} />
  </Provider>
);

export default AppRoot;
