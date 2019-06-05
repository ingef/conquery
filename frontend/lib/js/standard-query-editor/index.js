// @flow

import { combineReducers } from "redux";
import type { TabType } from "../common/types/tabs";

import { createQueryRunnerReducer } from "../query-runner";
import { default as queryReducer } from "./reducer";
import StandardQueryEditorTab from "./StandardQueryEditorTab";

const queryRunnerReducer = createQueryRunnerReducer("standard");

const Tab: TabType = {
  key: "queryEditor",
  label: "rightPane.queryEditor",
  reducer: combineReducers({
    query: queryReducer,
    queryRunner: queryRunnerReducer
  }),
  component: StandardQueryEditorTab
};

export default Tab;
