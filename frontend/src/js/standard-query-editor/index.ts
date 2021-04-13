import { combineReducers } from "redux";

import type { TabT } from "../pane/types";

import StandardQueryEditorTab from "./StandardQueryEditorTab";

import createQueryRunnerReducer from "../query-runner/reducer";
import { default as queryReducer } from "./queryReducer";
import selectedSecondaryIdsReducer from "./selectedSecondaryIdReducer";

import type { StandardQueryStateT } from "./queryReducer";
import type { SelectedSecondaryIdStateT } from "./selectedSecondaryIdReducer";
import type { QueryRunnerStateT } from "../query-runner/reducer";

const queryRunnerReducer = createQueryRunnerReducer("standard");

export interface StandardQueryEditorStateT {
  query: StandardQueryStateT;
  selectedSecondaryId: SelectedSecondaryIdStateT;
  queryRunner: QueryRunnerStateT;
}

const Tab: TabT = {
  key: "queryEditor",
  labelKey: "rightPane.queryEditor",
  tooltipKey: "help.tabQueryEditor",
  reducer: combineReducers({
    query: queryReducer,
    selectedSecondaryId: selectedSecondaryIdsReducer,
    queryRunner: queryRunnerReducer,
  }),
  component: StandardQueryEditorTab,
};

export default Tab;
