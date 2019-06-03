// @flow

import { createQueryRunnerReducer } from "../query-runner";
import { default as queryReducer } from "./reducer";
import StandardQueryEditorTab from "./StandardQueryEditorTab";

const queryRunnerReducer = createQueryRunnerReducer("standard");

const standardQueryEditorTabDescription = {
  key: "queryEditor",
  label: "rightPane.queryEditor"
};

export default {
  description: standardQueryEditorTabDescription,
  reducer: (state = standardQueryEditorTabDescription, action) => ({
    ...state,
    query: queryReducer(state.query, action),
    queryRunner: queryRunnerReducer(state.queryRunner, action)
  }),
  component: StandardQueryEditorTab
};
