import { applyMiddleware, createStore } from "redux";
import { composeWithDevTools } from "redux-devtools-extension";
import thunk from "redux-thunk";

import buildAppReducer from "./app/reducers";

export function makeStore(initialState: Object) {
  const middleware = applyMiddleware(thunk);
  const enhancer = composeWithDevTools(middleware);

  return createStore(buildAppReducer(), initialState, enhancer);
}
