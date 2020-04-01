import React from "react";
import type { Dispatch } from "redux-thunk";
import { connect } from "react-redux";

import { clickPaneTab } from "./actions";
import type { TabType } from "./reducer";

import TabNavigation from "./TabNavigation";

type PropsType = {
  tabs: TabType[];
  paneType: "left" | "right";
  activeTab: string;
  clickPaneTab: Function;
};

const PaneTabNavigation = (props: PropsType) => {
  return (
    <TabNavigation
      onClickTab={props.clickPaneTab}
      activeTab={props.activeTab}
      tabs={props.tabs}
    />
  );
};

const mapStateToProps = (state, ownProps) => ({
  tabs: state.panes[ownProps.paneType].tabs,
  activeTab: state.panes[ownProps.paneType].activeTab
});

const mapDispatchToProps = (dispatch: Dispatch, ownProps) => ({
  clickPaneTab: tab => dispatch(clickPaneTab(ownProps.paneType, tab))
});

export default connect(mapStateToProps, mapDispatchToProps)(PaneTabNavigation);
