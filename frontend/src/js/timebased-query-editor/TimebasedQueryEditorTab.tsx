import React from "react";

import TimebasedQueryEditor from "./TimebasedQueryEditor";
import TimebasedQueryClearButton from "./TimebasedQueryClearButton";
import TimebasedQueryRunner from "./TimebasedQueryRunner";

const TimebasedQueryEditorTab = () => (
  <>
    <TimebasedQueryClearButton />
    <TimebasedQueryEditor />
    <TimebasedQueryRunner />
  </>
);

export default TimebasedQueryEditorTab;
