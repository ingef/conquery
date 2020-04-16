import React from "react";
import { connect } from "react-redux";

import type { DatasetIdT } from "../api/types";

import Pane from "../pane/Pane";
import ConceptTreeList from "../concept-trees/ConceptTreeList";
import ConceptTreeSearchBox from "../concept-trees/ConceptTreeSearchBox";
import PreviousQueriesTab from "../previous-queries/list/PreviousQueriesTab";

type PropsType = {
  activeTab: string;
  selectedDatasetId: DatasetIdT | null;
};

const LeftPane = ({ activeTab, selectedDatasetId }: PropsType) => {
  return (
    <Pane left>
      {activeTab === "conceptTrees" && (
        <ConceptTreeSearchBox datasetId={selectedDatasetId} />
      )}
      <ConceptTreeList datasetId={selectedDatasetId} />
      {activeTab === "previousQueries" && (
        <PreviousQueriesTab datasetId={selectedDatasetId} />
      )}
    </Pane>
  );
};

export default connect(state => ({
  activeTab: state.panes.left.activeTab,
  selectedDatasetId: state.datasets.selectedDatasetId
}))(LeftPane);
