// @flow
import React, {PropsType}              from 'react';
import { Provider }                    from 'react-redux';

import AppRouter                       from './app/AppRouter';

const AppRoot = ({store, browserHistory}) => (
  <Provider store={store}>
    <AppRouter history={browserHistory} />
  </Provider>
);

AppRoot.propTypes = {
  store: PropsType.object.isRequired,
  browserHistory: PropsType.object.isRequired,
};

export default AppRoot;
