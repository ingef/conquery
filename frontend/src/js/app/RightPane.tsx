import styled from "@emotion/styled";
import type { StateT } from "app-types";
import React, { FC, useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import Pane from "../pane/Pane";
import { clickPaneTab } from "../pane/actions";
import type { TabT } from "../pane/types";

interface PropsT {
  tabs: TabT[];
}

// Shrink to size 0 so jQuery/cypress can differentiate between :hidden and :visible
const Tab = styled("div")<{ isActive: boolean }>`
  height: ${(isActive) => (isActive ? "100%" : 0 )};
  width: ${(isActive) => (isActive ? "100%" : 0 )};
  flex-grow: 1;
  flex-direction: column;

  display: ${({ isActive }) => (isActive ? "flex" : "none")};
`;

const RightPane: FC<PropsT> = ({ tabs }) => {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const activeTab = useSelector<StateT, string | null>(
    (state) => state.panes.right.activeTab,
  );

  useEffect(() => {
    dispatch(clickPaneTab({ paneType: "right", tab: tabs[0].key }));
  }, [dispatch, tabs]);

  return (
    <Pane
      right
      tabs={tabs.map((tab) => ({
        key: tab.key,
        label: t(tab.labelKey), // TODO: Somehow make this non-dynamic
        tooltip: t(tab.tooltipKey), // TODO: Somehow make this non-dynamic
      }))}
      dataTestId="rightPane"
    >
      {tabs.map((tab) => {
        const isActive = tab.key === activeTab;
        const tabComponent = React.createElement(tab.component);

        return (
          <Tab key={tab.key} isActive={isActive}>
            {tabComponent}
          </Tab>
        );
      })}
    </Pane>
  );
};

export default RightPane;
