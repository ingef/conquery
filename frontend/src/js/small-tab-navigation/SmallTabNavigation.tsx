import React, { FC } from "react";

import SmallTabNavigationButton from "./SmallTabNavigationButton";

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
    <div className={className}>
      {options.map((option) => (
        <SmallTabNavigationButton
          key={option.value}
          value={option.value}
          isSelected={selectedTab === option.value}
          onClick={() => onSelectTab(option.value)}
        >
          {option.label}
        </SmallTabNavigationButton>
      ))}
    </div>
  );
};

export default SmallTabNavigation;
