import { createStore } from "redux";
import { composeWithDevTools } from "redux-devtools-extension";

import buildAppReducer from "./app/reducers";

export function makeStore(initialState: Object) {
  const enhancer = composeWithDevTools();

  return createStore(buildAppReducer(), initialState, enhancer);
}
