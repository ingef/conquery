// @flow
import React                           from 'react';
import PropTypes                       from 'prop-types';
import { Provider }                    from 'react-redux';
import { hot }                         from 'react-hot-loader'

import AppRouter                       from './app/AppRouter';

const AppRoot = ({store, browserHistory}) => (
  <Provider store={store}>
    <AppRouter history={browserHistory} />
  </Provider>
);

AppRoot.propTypes = {
  store: PropTypes.object.isRequired,
  browserHistory: PropTypes.object.isRequired,
};

export default hot(module)(AppRoot);
