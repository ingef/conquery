import { combineReducers } from "redux";

import type { TabT } from "../pane/types";
import createQueryRunnerReducer, {
  QueryRunnerStateT,
} from "../query-runner/reducer";
import {
  default as timebasedQueryReducer,
  TimebasedQueryStateT,
} from "./reducer";
import TimebasedQueryEditorTab from "./TimebasedQueryEditorTab";

const timebasedQueryRunnerReducer = createQueryRunnerReducer("timebased");

export interface TimebasedQueryEditorStateT {
  timebasedQuery: TimebasedQueryStateT;
  timebasedQueryRunner: QueryRunnerStateT;
}

const Tab: TabT = {
  key: "timebasedQueryEditor",
  labelKey: "rightPane.timebasedQueryEditor",
  tooltipKey: "help.tabTimebasedEditor",
  reducer: combineReducers({
    timebasedQuery: timebasedQueryReducer,
    timebasedQueryRunner: timebasedQueryRunnerReducer,
  }),
  component: TimebasedQueryEditorTab,
};

export default Tab;
