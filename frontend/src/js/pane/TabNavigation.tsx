import { faSpinner } from "@fortawesome/free-solid-svg-icons";
import { Focusable } from "react-aria-components";
import { tv } from "tailwind-variants";
import FaIcon from "../icon/FaIcon";
import { HoverNavigatable } from "../small-tab-navigation/HoverNavigatable";
import {
  Tooltip,
  TooltipTrigger,
  tooltipDelay,
} from "../ui-components/Tooltip";

const root = tv({
  base: ["flex items-start", "border-b border-gray-100", "bg-white", "px-5"],
});

const headline = tv({
  base: [
    "mt-[6px] mr-[5px] mb-0",
    "px-3",
    "shrink-0",
    "cursor-pointer",
    "border-b-[3px]",
    "transition-colors",
    "text-sm",
    "leading-[30px]",
    "font-bold",
    "uppercase",
    "tracking-wider",
  ],
  variants: {
    active: {
      true: "border-primary-500 text-primary-500",
      false:
        "border-transparent text-gray-500 hover:border-primary-200 hover:text-black",
    },
  },
});

export interface TabNavigationTab {
  key: string;
  label: string;
  tooltip?: string;
  loading?: boolean;
}

const TabNavigation = ({
  tabs,
  activeTab,
  onClickTab,
  dataTestId,
}: {
  onClickTab: (tab: string) => void;
  activeTab: string | null;
  tabs: TabNavigationTab[];
  dataTestId: string;
}) => {
  function createClickHandler(key: string) {
    return () => {
      if (key !== activeTab) {
        onClickTab(key);
      }
    };
  }

  return (
    <div className={root()} data-test-id={dataTestId}>
      {tabs.map(({ key, label, tooltip, loading }) => {
        return (
          <HoverNavigatable key={key} triggerNavigate={createClickHandler(key)}>
            <TooltipTrigger delay={tooltipDelay.info}>
              <Focusable>
                <button
                  type="button"
                  className={headline({ active: activeTab === key })}
                  onClick={createClickHandler(key)}
                >
                  {label}
                  {loading && <FaIcon className="ml-[5px]" icon={faSpinner} />}
                </button>
              </Focusable>
              <Tooltip>{tooltip}</Tooltip>
            </TooltipTrigger>
          </HoverNavigatable>
        );
      })}
    </div>
  );
};

export default TabNavigation;
