// @flow

import React from "react";
import { connect } from "react-redux";

import { type DatasetIdType } from "../dataset/reducer";

import { Pane } from "../pane";
import { CategoryTreeList, CategoryTreeSearchBox } from "../category-trees";
import PreviousQueriesTab from "../previous-queries/list/PreviousQueriesTab";

type PropsType = {
  activeTab: string,
  selectedDatasetId: ?DatasetIdType
};

const LeftPane = ({ activeTab, selectedDatasetId }: PropsType) => {
  return (
    <Pane left>
      {activeTab === "categoryTrees" && (
        <CategoryTreeSearchBox datasetId={selectedDatasetId} />
      )}
      <CategoryTreeList />
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
