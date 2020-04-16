import React from "react";
import styled from "@emotion/styled";
import T from "i18n-react";
import type { TabType } from "./reducer";

const Root = styled("div")`
  margin-bottom: 10px;
  border-bottom: 1px solid ${({ theme }) => theme.col.grayLight};
  padding: 0 20px;
  background-color: white;
`;

const Headline = styled("h2")`
  font-size: ${({ theme }) => theme.font.sm};
  margin-bottom: 0;
  margin-top: 5px;
  padding: 0 12px;
  letter-spacing: 1px;
  line-height: 38px;
  text-transform: uppercase;

  transition: color ${({ theme }) => theme.transitionTime},
    border-bottom ${({ theme }) => theme.transitionTime};
  cursor: pointer;
  display: inline-block;
  margin-right: 15px;
  color: ${({ theme, active }) =>
    active ? theme.col.blueGrayDark : theme.col.gray};
  border-bottom: 3px solid
    ${({ theme, active }) => (active ? theme.col.blueGrayDark : "transparent")};

  &:hover {
    color: ${({ theme, active }) =>
      active ? theme.col.blueGrayDark : theme.col.black};
    border-bottom: 3px solid
      ${({ theme, active }) =>
        active ? theme.col.blueGrayDark : theme.col.grayLight};
  }
`;

type PropsType = {
  onClickTab: Function;
  activeTab: string;
  tabs: TabType[];
};

const TabNavigation = (props: PropsType) => {
  return (
    <Root>
      {Object.values(props.tabs).map(({ label, key }) => (
        <Headline
          key={key}
          active={props.activeTab === key}
          onClick={() => {
            if (key !== props.activeTab) props.onClickTab(key);
          }}
        >
          {T.translate(label)}
        </Headline>
      ))}
    </Root>
  );
};

export default TabNavigation;
