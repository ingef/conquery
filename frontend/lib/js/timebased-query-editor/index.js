import React from "react";

import { type TabPropsType } from "../pane";
import { createQueryRunnerReducer } from "../query-runner";

import { default as timebasedQueryReducer } from "./reducer";

import TimebasedQueryEditor from "./TimebasedQueryEditor";
import TimebasedQueryClearButton from "./TimebasedQueryClearButton";
import TimebasedQueryRunner from "./TimebasedQueryRunner";

const timebasedQueryRunnerReducer = createQueryRunnerReducer("timebased");

const timebasedQueryEditorTabDescription = {
  key: "timebasedQueryEditor",
  label: "rightPane.timebasedQueryEditor"
};

export const TimebasedQueryEditorTab = {
  description: timebasedQueryEditorTabDescription,
  reducer: (state = timebasedQueryEditorTabDescription, action) => ({
    ...state,
    timebasedQuery: timebasedQueryReducer(state.timebasedQuery, action),
    timebasedQueryRunner: timebasedQueryRunnerReducer(
      state.timebasedQueryRunner,
      action
    )
  }),
  component: (props: TabPropsType) => (
    <>
      <TimebasedQueryClearButton />
      <TimebasedQueryEditor />
      <TimebasedQueryRunner datasetId={props.selectedDatasetId} />
    </>
  )
};

export * as actions from "./actions";
