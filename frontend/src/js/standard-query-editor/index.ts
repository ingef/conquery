import { combineReducers } from "redux";
import type { TabT } from "../pane/types";

import createQueryRunnerReducer from "../query-runner/reducer";
import { default as queryReducer } from "./reducer";
import StandardQueryEditorTab from "./StandardQueryEditorTab";

import type { StandardQueryStateT } from "./reducer";
import type { QueryRunnerStateT } from "../query-runner/reducer";

const queryRunnerReducer = createQueryRunnerReducer("standard");

export interface StandardQueryEditorStateT {
  query: StandardQueryStateT;
  queryRunner: QueryRunnerStateT;
}

const Tab: TabT = {
  key: "queryEditor",
  label: "rightPane.queryEditor",
  reducer: combineReducers({
    query: queryReducer,
    queryRunner: queryRunnerReducer
  }),
  component: StandardQueryEditorTab
};

export default Tab;
