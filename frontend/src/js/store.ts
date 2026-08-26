import { composeWithDevTools } from "@redux-devtools/extension";
import { legacy_createStore as createStore } from "redux";

import buildAppReducer from "./app/reducers";

export function makeStore() {
  const enhancer = composeWithDevTools();

  return createStore(buildAppReducer(), {}, enhancer);
}
