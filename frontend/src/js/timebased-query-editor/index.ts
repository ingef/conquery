import { combineReducers } from "redux";

import type { TabT } from "../pane/types";
import createQueryRunnerReducer from "../query-runner/reducer";
import { default as timebasedQueryReducer } from "./reducer";
import TimebasedQueryEditorTab from "./TimebasedQueryEditorTab";

const timebasedQueryRunnerReducer = createQueryRunnerReducer("timebased");

const Tab: TabT = {
  key: "timebasedQueryEditor",
  label: "rightPane.timebasedQueryEditor",
  reducer: combineReducers({
    timebasedQuery: timebasedQueryReducer,
    timebasedQueryRunner: timebasedQueryRunnerReducer
  }),
  component: TimebasedQueryEditorTab
};

export default Tab;
