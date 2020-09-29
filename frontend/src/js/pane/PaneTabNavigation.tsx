import React from "react";
import { useSelector, useDispatch } from "react-redux";

import { clickPaneTab } from "./actions";
import type { TabType } from "./reducer";

import TabNavigation from "./TabNavigation";
import { StateT } from "app-types";

interface PropsT {
  paneType: "left" | "right";
}

const PaneTabNavigation: React.FC<PropsT> = ({ paneType }) => {
  const tabs = useSelector<StateT, TabType[]>(
    state => state.panes[paneType].tabs
  );
  const activeTab = useSelector<StateT, string>(
    state => state.panes[paneType].activeTab
  );
  const dispatch = useDispatch();

  const onClickTab = (tab: string) => dispatch(clickPaneTab(paneType, tab));

  return (
    <TabNavigation onClickTab={onClickTab} activeTab={activeTab} tabs={tabs} />
  );
};

export default PaneTabNavigation;
