import styled from "@emotion/styled";
import React, { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import Pane from "../pane/Pane";
import { clickPaneTab } from "../pane/actions";
import type { TabT } from "../pane/types";

import type { StateT } from "./reducers";

const Tab = styled("div")<{ isActive: boolean }>`
  height: 100%;
  flex-grow: 1;
  flex-direction: column;

  display: ${({ isActive }) => (isActive ? "flex" : "none")};
`;

const SxPane = styled(Pane)`
  background-color: ${({ theme }) => theme.col.bgAlt};
`;

const RightPane = ({ tabs }: { tabs: TabT[] }) => {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const activeTab = useSelector<StateT, string | null>(
    (state) => state.panes.right.activeTab,
  );

  useEffect(() => {
    dispatch(clickPaneTab({ paneType: "right", tab: tabs[0].key }));
  }, [dispatch, tabs]);

  return (
    <SxPane
      right
      tabs={tabs.map((tab) => ({
        key: tab.key,
        label: t(tab.labelKey), // TODO: Somehow make this non-dynamic
        tooltip: t(tab.tooltipKey), // TODO: Somehow make this non-dynamic
      }))}
      dataTestId="right-pane"
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
    </SxPane>
  );
};

export default RightPane;
