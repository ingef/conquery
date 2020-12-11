import React from "react";

import type { TabPropsType } from "../pane";
import { QueryEditor } from "./QueryEditor";
import { StandardQueryRunner } from "./StandardQueryRunner";

const StandardQueryEditorTab = (props: TabPropsType) => (
  <>
    <QueryEditor selectedDatasetId={props.selectedDatasetId} />
    <StandardQueryRunner datasetId={props.selectedDatasetId} />
  </>
);

export default StandardQueryEditorTab;
