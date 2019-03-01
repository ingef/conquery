import thunk from "redux-thunk";
import multi from "redux-multi";
import { routerMiddleware } from "react-router-redux";

export default function(browserHistory) {
  const reduxRouterMiddleware = routerMiddleware(browserHistory);

  return [thunk, multi, reduxRouterMiddleware];
}
