import thunk from "redux-thunk";
import multi from "redux-multi";
import { routerMiddleware } from "react-router-redux";
import { createUnauthorizedErrorMiddleware } from "../authorization";

export default function(browserHistory) {
  const reduxRouterMiddleware = routerMiddleware(browserHistory);
  const unauthorizedErrorMiddleware = createUnauthorizedErrorMiddleware();

  return [thunk, multi, unauthorizedErrorMiddleware, reduxRouterMiddleware];
}
