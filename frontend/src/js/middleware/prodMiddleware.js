import thunk                 from 'redux-thunk';
import { routerMiddleware }  from 'react-router-redux';

export default function(browserHistory) {
  const reduxRouterMiddleware = routerMiddleware(browserHistory);

  return [
    thunk,
    reduxRouterMiddleware,
  ];
}
