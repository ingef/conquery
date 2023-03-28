import styled from "@emotion/styled";
import React, { useEffect } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";

import FormsTab from "../external-forms/FormsTab";
import Pane from "../pane/Pane";
import { clickPaneTab } from "../pane/actions";
import { TabT } from "../pane/types";
import StandardQueryEditorTab from "../standard-query-editor/StandardQueryEditorTab";
import TimebasedQueryEditorTab from "../timebased-query-editor/TimebasedQueryEditorTab";

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

const tabs: TabT[] = [
  {
    key: "queryEditor",
    labelKey: "rightPane.queryEditor",
    tooltipKey: "help.tabQueryEditor",
    component: StandardQueryEditorTab,
  },
  {
    key: "timebasedQueryEditor",
    labelKey: "rightPane.timebasedQueryEditor",
    tooltipKey: "help.tabTimebasedEditor",
    component: TimebasedQueryEditorTab,
  },
  {
    key: "externalForms",
    labelKey: "rightPane.externalForms",
    tooltipKey: "help.tabFormEditor",
    component: FormsTab,
  },
];

const RightPane = () => {
  const { t } = useTranslation();
  const dispatch = useDispatch();
  const activeTab = useSelector<StateT, string | null>(
    (state) => state.panes.right.activeTab,
  );

  useEffect(() => {
    dispatch(clickPaneTab({ paneType: "right", tab: tabs[0].key }));
  }, [dispatch]);

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
