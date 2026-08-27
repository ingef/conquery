import styled from "@emotion/styled";
import { faSpinner } from "@fortawesome/free-solid-svg-icons";
import FaIcon from "../icon/FaIcon";
import { HoverNavigatable } from "../small-tab-navigation/HoverNavigatable";
import WithTooltip from "../tooltip/WithTooltip";
import { tv } from "../tv";

const Root = styled("div")`
  border-bottom: 1px solid ${({ theme }) => theme.col.grayLight};
  padding: 0 20px;
  background-color: white;
  display: flex;
  align-items: flex-start;
`;

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
    <Root data-test-id={dataTestId}>
      {tabs.map(({ key, label, tooltip, loading }) => {
        return (
          <HoverNavigatable key={key} triggerNavigate={createClickHandler(key)}>
            <SxWithTooltip text={tooltip} lazy>
              <button
                type="button"
                className={headline({ active: activeTab === key })}
                onClick={createClickHandler(key)}
              >
                {label}
                {loading && <SxFaIcon icon={faSpinner} />}
              </button>
            </SxWithTooltip>
          </HoverNavigatable>
        );
      })}
    </Root>
  );
};

export default TabNavigation;
