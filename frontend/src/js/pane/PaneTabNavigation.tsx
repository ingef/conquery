import { StateT } from "app-types";
import { FC } from "react";
import { useSelector, useDispatch } from "react-redux";

import TabNavigation, { TabNavigationTab } from "./TabNavigation";
import { clickPaneTab } from "./actions";

interface PropsT {
  paneType: "left" | "right";
  tabs: TabNavigationTab[];
  dataTestId: string;
}

const PaneTabNavigation: FC<PropsT> = ({ tabs, paneType , dataTestId}) => {
  const activeTab = useSelector<StateT, string | null>(
    (state) => state.panes[paneType].activeTab,
  );
  const dispatch = useDispatch();

  const onClickTab = (tab: string) => dispatch(clickPaneTab({ paneType, tab }));

  return (
    <TabNavigation onClickTab={onClickTab} activeTab={activeTab} tabs={tabs} dataTestId={dataTestId} />
  );
};

export default PaneTabNavigation;
