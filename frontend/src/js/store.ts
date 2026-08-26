import { composeWithDevTools } from "@redux-devtools/extension";
import { createStore } from "redux";

import buildAppReducer from "./app/reducers";

export function makeStore() {
  const enhancer = composeWithDevTools();

  return createStore(buildAppReducer(), {}, enhancer);
}
