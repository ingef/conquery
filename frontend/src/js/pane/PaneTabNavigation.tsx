import { useDispatch, useSelector } from "react-redux";

import type { StateT } from "../app/reducers";
import { clickPaneTab } from "./actions";
import TabNavigation, { type TabNavigationTab } from "./TabNavigation";

const PaneTabNavigation = ({
  tabs,
  paneType,
  dataTestId,
}: {
  paneType: "left" | "right";
  tabs: TabNavigationTab[];
  dataTestId: string;
}) => {
  const activeTab = useSelector<StateT, string | null>(
    (state) => state.panes[paneType].activeTab,
  );
  const dispatch = useDispatch();

  const onClickTab = (tab: string) => dispatch(clickPaneTab({ paneType, tab }));

  return (
    <TabNavigation
      onClickTab={onClickTab}
      activeTab={activeTab}
      tabs={tabs}
      dataTestId={dataTestId}
    />
  );
};

export default PaneTabNavigation;
