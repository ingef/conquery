// @flow

import React from "react";
import { connect } from "react-redux";

import { Pane } from "../pane";
import { getRightPaneTabComponent } from "../pane/reducer";

type PropsType = {
  activeTab: string,
  selectedDatasetId: ?string
};

const RightPane = (props: PropsType) => {
  const tab = React.createElement(getRightPaneTabComponent(props.activeTab), {
    selectedDatasetId: props.selectedDatasetId
  });

  return <Pane right>{tab}</Pane>;
};

export default connect(state => ({
  activeTab: state.panes.right.activeTab,
  selectedDatasetId: state.datasets.selectedDatasetId
}))(RightPane);
