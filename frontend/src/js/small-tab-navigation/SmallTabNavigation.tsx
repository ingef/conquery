import { FC, ReactNode } from "react";

import WithTooltip from "../tooltip/WithTooltip";

import SmallTabNavigationButton from "./SmallTabNavigationButton";

interface TabOption {
  label: ReactNode;
  value: string;
  tooltip?: string;
}

interface PropsT {
  className?: string;
  size?: "M" | "L";
  options: TabOption[];
  selectedTab: string;
  onSelectTab: (tab: string) => void;
}

const SmallTabNavigation: FC<PropsT> = ({
  className,
  size = "M",
  options,
  selectedTab,
  onSelectTab,
}) => {
  return (
    <div className={className}>
      {options.map((option) => (
        <WithTooltip key={option.value} text={option.tooltip}>
          <SmallTabNavigationButton
            key={option.value}
            value={option.value}
            size={size}
            isSelected={selectedTab === option.value}
            onClick={() => onSelectTab(option.value)}
          >
            {option.label}
          </SmallTabNavigationButton>
        </WithTooltip>
      ))}
    </div>
  );
};

export default SmallTabNavigation;
