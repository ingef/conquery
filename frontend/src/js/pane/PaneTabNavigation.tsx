import React from "react";
import { useSelector, useDispatch } from "react-redux";

import { clickPaneTab } from "./actions";

import TabNavigation from "./TabNavigation";
import { StateT } from "app-types";

interface PropsT {
  paneType: "left" | "right";
  tabs: { key: string; label: string }[];
}

const PaneTabNavigation: React.FC<PropsT> = ({ tabs, paneType }) => {
  const activeTab = useSelector<StateT, string | null>(
    (state) => state.panes[paneType].activeTab
  );
  const dispatch = useDispatch();

  const onClickTab = (tab: string) => dispatch(clickPaneTab(paneType, tab));

  return (
    <TabNavigation onClickTab={onClickTab} activeTab={activeTab} tabs={tabs} />
  );
};

export default PaneTabNavigation;
