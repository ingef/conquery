import React, { FC } from "react";
import { useSelector } from "react-redux";
import styled from "@emotion/styled";
import type { StateT } from "app-types";

import type { TabT } from "../pane/types";

import Pane from "../pane/Pane";

interface PropsT {
  tabs: TabT[];
}

const Tab = styled("div")<{ isActive: boolean }>`
  height: 100%;
  flex-grow: 1;
  display: flex;
  flex-direction: column;

  display: ${({ isActive }) => (isActive ? "flex" : "none")};
`;

const RightPane: FC<PropsT> = ({ tabs }) => {
  const activeTab = useSelector<StateT, string>(
    (state) => state.panes.right.activeTab
  );
  const selectedDatasetId = useSelector<StateT, string | null>(
    (state) => state.datasets.selectedDatasetId
  );

  return (
    <Pane right>
      {tabs.map((tab) => {
        const isActive = tab.key === activeTab;
        const tabComponent = React.createElement(tab.component, {
          selectedDatasetId: selectedDatasetId,
        });

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
