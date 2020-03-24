import React from "react";

import type { TabPropsType } from "../pane";
import { QueryEditor } from "./QueryEditor";
import { StandardQueryRunner } from "./StandardQueryRunner";
import { QueryClearButton } from "./QueryClearButton";

const StandardQueryEditorTab = (props: TabPropsType) => (
  <>
    <QueryClearButton />
    <QueryEditor selectedDatasetId={props.selectedDatasetId} />
    <StandardQueryRunner datasetId={props.selectedDatasetId} />
  </>
);

export default StandardQueryEditorTab;
