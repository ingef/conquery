// @flow

import React from "react";
import { connect } from "react-redux";
import type { TabType } from "../pane/types";

import { Pane } from "../pane";

type PropsType = {
  tabs: TabType[],
  activeTab: string,
  selectedDatasetId: ?string
};

const RightPane = ({ tabs, activeTab, selectedDatasetId }: PropsType) => {
  // Won't be null, since the activeTabs string is built from tabs
  // during setup
  const theActiveTab = tabs.find(tab => tab.key === activeTab);

  const tab = React.createElement(theActiveTab.component, {
    selectedDatasetId: selectedDatasetId
  });

  return <Pane right>{tab}</Pane>;
};

export default connect(state => ({
  activeTab: state.panes.right.activeTab,
  selectedDatasetId: state.datasets.selectedDatasetId
}))(RightPane);
