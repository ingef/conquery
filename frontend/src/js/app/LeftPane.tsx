import React from "react";
import { useSelector } from "react-redux";

import type { DatasetIdT } from "../api/types";

import Pane from "../pane/Pane";
import ConceptTreeList from "../concept-trees/ConceptTreeList";
import ConceptTreeSearchBox from "../concept-trees/ConceptTreeSearchBox";
import PreviousQueriesTab from "../previous-queries/list/PreviousQueriesTab";
import FormConfigsTab from "js/external-forms/form-configs/FormConfigsTab";
import { StateT } from "./reducers";

const LeftPane = () => {
  const activeTab = useSelector<StateT, string>(
    state => state.panes.left.activeTab
  );
  const selectedDatasetId = useSelector<StateT, DatasetIdT | null>(
    state => state.datasets.selectedDatasetId
  );

  return (
    <Pane left>
      {activeTab === "conceptTrees" && (
        <ConceptTreeSearchBox datasetId={selectedDatasetId} />
      )}
      <ConceptTreeList datasetId={selectedDatasetId} />
      {activeTab === "previousQueries" && (
        <PreviousQueriesTab datasetId={selectedDatasetId} />
      )}
      {activeTab === "formConfigs" && (
        <FormConfigsTab datasetId={selectedDatasetId} />
      )}
    </Pane>
  );
};

export default LeftPane;
