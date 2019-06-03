import { createQueryRunnerReducer } from "../query-runner";
import { default as timebasedQueryReducer } from "./reducer";
import TimebasedQueryEditorTab from "./TimebasedQueryEditorTab";

const timebasedQueryRunnerReducer = createQueryRunnerReducer("timebased");

const timebasedQueryEditorTabDescription = {
  key: "timebasedQueryEditor",
  label: "rightPane.timebasedQueryEditor"
};

export * as actions from "./actions";

export default {
  description: timebasedQueryEditorTabDescription,
  reducer: (state = timebasedQueryEditorTabDescription, action) => ({
    ...state,
    timebasedQuery: timebasedQueryReducer(state.timebasedQuery, action),
    timebasedQueryRunner: timebasedQueryRunnerReducer(
      state.timebasedQueryRunner,
      action
    )
  }),
  component: TimebasedQueryEditorTab
};
