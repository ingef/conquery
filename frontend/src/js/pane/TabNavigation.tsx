import styled from "@emotion/styled";
import { faSpinner } from "@fortawesome/free-solid-svg-icons";

import tw from "tailwind-styled-components";
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

const Headline = tw("h2")<{ $active: boolean }>`
  text-sm
  mb-0
  mt-[6px]
  mr-[5px]
  px-3
  font-bold
  leading-[30px]
  uppercase
  flex-shrink-0
  transition-colors
  cursor-pointer
  tracking-wider

  border-b-[3px]
  ${({ $active }) =>
    $active ? "text-primary-500" : "text-gray-500 hover:text-black"};
  ${({ $active }) =>
    $active
      ? "border-primary-500"
      : "border-transparent hover:border-primary-200"};

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
              <Headline
                $active={activeTab === key}
                onClick={createClickHandler(key)}
              >
                {label}
                {loading && <SxFaIcon icon={faSpinner} />}
              </Headline>
            </SxWithTooltip>
          </HoverNavigatable>
        );
      })}
    </Root>
  );
};

export default TabNavigation;
