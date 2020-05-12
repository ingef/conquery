import React, { FC } from "react";

import SmallTabNavigationButton from "./SmallTabNavigationButton";
import styled from "@emotion/styled";

const Root = styled("div")`
  margin-bottom: 5px;
  padding: 0 10px;
`;

interface TabOption {
  label: string;
  value: string;
}

interface PropsT {
  className?: string;
  options: TabOption[];
  selectedTab: string;
  onSelectTab: (tab: string) => void;
}

const SmallTabNavigation: FC<PropsT> = ({
  className,
  options,
  selectedTab,
  onSelectTab,
}) => {
  return (
    <Root className={className}>
      {options.map((option) => (
        <SmallTabNavigationButton
          value={option.value}
          isSelected={selectedTab === option.value}
          onClick={() => onSelectTab(option.value)}
        >
          {option.label}
        </SmallTabNavigationButton>
      ))}
    </Root>
  );
};

export default SmallTabNavigation;
