import styled from "@emotion/styled";
import { FC } from "react";

import FaIcon from "../icon/FaIcon";
import { HoverNavigatable } from "../small-tab-navigation/HoverNavigatable";
import WithTooltip from "../tooltip/WithTooltip";

const Root = styled("div")`
  border-bottom: 1px solid ${({ theme }) => theme.col.grayLight};
  padding: 0 20px;
  background-color: white;
  display: flex;
  align-items: flex-start;
`;

const Headline = styled("h2")<{ active: boolean }>`
  font-size: ${({ theme }) => theme.font.sm};
  margin-bottom: 0;
  margin-top: 6px;
  padding: 0 12px;
  letter-spacing: 1px;
  line-height: 30px;
  text-transform: uppercase;
  flex-shrink: 0;

  transition: color ${({ theme }) => theme.transitionTime},
    border-bottom ${({ theme }) => theme.transitionTime};
  cursor: pointer;
  margin-right: 5px;
  color: ${({ theme, active }) =>
    active ? theme.col.blueGrayDark : theme.col.gray};
  border-bottom: 4px solid
    ${({ theme, active }) => (active ? theme.col.blueGrayDark : "transparent")};

  &:hover {
    color: ${({ theme, active }) =>
      active ? theme.col.blueGrayDark : theme.col.black};
    border-bottom: 4px solid
      ${({ theme, active }) =>
        active ? theme.col.blueGrayDark : theme.col.grayLight};
  }
`;

const SxWithTooltip = styled(WithTooltip)`
  flex-shrink: 0;
`;

const SxFaIcon = styled(FaIcon)`
  margin-left: 5px;
`;

export interface TabNavigationTab {
  key: string;
  label: string;
  tooltip?: string;
  loading?: boolean;
}

interface PropsT {
  onClickTab: (tab: string) => void;
  activeTab: string | null;
  tabs: TabNavigationTab[];
  dataTestId: string;
}

const TabNavigation: FC<PropsT> = ({ tabs, activeTab, onClickTab, dataTestId }) => {

  function createClickHandler(key: string){
    return () => {
      if(key !== activeTab){
        onClickTab(key);
      }
    }
  }

  return (
    <Root data-test-id={dataTestId}>
      {tabs.map(({ key, label, tooltip, loading }) => {
        return (
          <SxWithTooltip text={tooltip} lazy key={key}>
            <HoverNavigatable triggerNavigate={createClickHandler(key)} >
              <Headline
                active={activeTab === key}
                onClick={createClickHandler(key)}
              >
                {label}
                {loading && <SxFaIcon icon="spinner" />}
              </Headline>
            </HoverNavigatable>
          </SxWithTooltip>
        );
      })}
    </Root>
  );
};

export default TabNavigation;
