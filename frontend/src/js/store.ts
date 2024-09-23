import { createStore } from "redux";
import { composeWithDevTools } from "redux-devtools-extension";

import buildAppReducer from "./app/reducers";

export function makeStore() {
  const enhancer = composeWithDevTools();

  return createStore(buildAppReducer(), {}, enhancer);
}
