// @flow

import React from "react";
import { Route } from "react-router";

import { Pane } from "../pane";
import { templates } from "../routes";
import { getRightPaneTabComponent } from "../pane/reducer";

type PropsType = {
  activeTab: string,
  tabs: Object
};

const RightPane = (props: PropsType) => (
  <Route
    path={templates.toDataset}
    children={({ match }) => {
      const selectedDatasetId =
        match && match.params ? match.params.datasetId : null;

      const tab = React.createElement(
        getRightPaneTabComponent(props.activeTab),
        { selectedDatasetId }
      );

      return <Pane type="right">{tab}</Pane>;
    }}
  />
);

export default RightPane;
