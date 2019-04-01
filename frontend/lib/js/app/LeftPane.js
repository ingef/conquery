// @flow

import React from "react";
import { connect } from "react-redux";

import { type DatasetIdType } from "../dataset/reducer";

import { Pane } from "../pane";
import { CategoryTreeList, CategoryTreeSearchBox } from "../category-trees";
import { DeletePreviousQueryModal } from "../previous-queries/delete-modal";
import { PreviousQueriesSearchBox } from "../previous-queries/search";
import { PreviousQueriesFilter } from "../previous-queries/filter";
import { PreviousQueriesContainer } from "../previous-queries/list";
import { UploadQueryResults } from "../previous-queries/upload";

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
      {activeTab === "previousQueries" && [
        <PreviousQueriesFilter key={0} />,
        <PreviousQueriesSearchBox key={1} isMulti />,
        <UploadQueryResults datasetId={selectedDatasetId} key={2} />,
        <PreviousQueriesContainer datasetId={selectedDatasetId} key={3} />,
        <DeletePreviousQueryModal datasetId={selectedDatasetId} key={4} />
      ]}
    </Pane>
  );
};

export default connect(state => ({
  activeTab: state.panes.left.activeTab,
  selectedDatasetId: state.datasets.selectedDatasetId
}))(LeftPane);
