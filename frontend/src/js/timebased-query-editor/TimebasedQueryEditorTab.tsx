import React from "react";

import type { TabPropsType } from "../pane";

import TimebasedQueryEditor from "./TimebasedQueryEditor";
import TimebasedQueryClearButton from "./TimebasedQueryClearButton";
import TimebasedQueryRunner from "./TimebasedQueryRunner";

const TimebasedQueryEditorTab = (props: TabPropsType) => (
  <>
    <TimebasedQueryClearButton />
    <TimebasedQueryEditor />
    <TimebasedQueryRunner datasetId={props.selectedDatasetId} />
  </>
);

export default TimebasedQueryEditorTab;
