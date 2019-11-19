// @flow

import React from "react";
import { connect } from "react-redux";

import type { DatasetIdT } from "../api/types";

import { Pane } from "../pane";
import { ConceptTreeList, ConceptTreeSearchBox } from "../concept-trees";
import PreviousQueriesTab from "../previous-queries/list/PreviousQueriesTab";

type PropsType = {
  activeTab: string,
  selectedDatasetId: ?DatasetIdT
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
