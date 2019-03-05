// @flow

import React from "react";
import { connect } from "react-redux";

import { Pane } from "../pane";
import { getRightPaneTabComponent } from "../pane/reducer";

type PropsType = {
  activeTab: string,
  tabs: Object
};

const RightPane = (props: PropsType) => {
  const tab = React.createElement(getRightPaneTabComponent(props.activeTab), {
    selectedDatasetId: props.selectedDatasetId
  });

  return <Pane type="right">{tab}</Pane>;
};

export default connect(state => ({
  activeTab: state.panes.right.activeTab,
  rightPaneTabs: state.panes.right.tabs,
  selectedDatasetId: state.datasets.selectedDatasetId
}))(RightPane);
