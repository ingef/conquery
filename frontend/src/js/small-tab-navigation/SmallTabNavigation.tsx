import { ReactNode } from "react";

import WithTooltip from "../tooltip/WithTooltip";

import SmallTabNavigationButton from "./SmallTabNavigationButton";

interface TabOption {
  label: ({ selected }: { selected?: boolean }) => ReactNode;
  value: string;
  tooltip?: string;
}

const SmallTabNavigation = ({
  className,
  size = "M",
  variant = "secondary",
  options,
  selectedTab,
  onSelectTab,
}: {
  className?: string;
  size?: "M" | "L";
  variant?: "primary" | "secondary";
  options: TabOption[];
  selectedTab: string;
  onSelectTab: (tab: string) => void;
}) => {
  return (
    <div className={className}>
      {options.map((option) => {
        const selected = option.value === selectedTab;

        return (
          <WithTooltip key={option.value} text={option.tooltip}>
            <SmallTabNavigationButton
              variant={variant}
              key={option.value}
              value={option.value}
              size={size}
              isSelected={selected}
              onClick={() => onSelectTab(option.value)}
            >
              {option.label({ selected })}
            </SmallTabNavigationButton>
          </WithTooltip>
        );
      })}
    </div>
  );
};

export default SmallTabNavigation;
