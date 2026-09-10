import type { ReactNode } from "react";
import { Focusable } from "react-aria-components";

import { Tooltip, TooltipTrigger } from "../ui-components/Tooltip";

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
          <TooltipTrigger key={option.value}>
            <Focusable>
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
            </Focusable>
            <Tooltip>{option.tooltip}</Tooltip>
          </TooltipTrigger>
        );
      })}
    </div>
  );
};

export default SmallTabNavigation;
