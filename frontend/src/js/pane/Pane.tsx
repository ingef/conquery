import type { ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import { tv } from "tailwind-variants";

import type { StateT } from "../app/reducers";
import { Tab, TabList, TabPanel, Tabs } from "../ui-components/Tabs";
import { clickPaneTab } from "./actions";

// starts below the absolutely positioned header
const root = tv({ base: ["h-full w-full", "pt-header"] });

// the content row, a column of the panels; the selected one takes it
const container = tv({
  base: ["relative", "flex flex-col", "min-h-0", "overflow-hidden"],
});

// sits in the tab row, before the tabs
const beforeTabsBox = tv({
  base: ["flex items-center", "pl-[10px]", "border-b border-gray-100"],
});

export interface PaneTab {
  key: string;
  label: string;
  tooltip?: string;
  content: ReactNode;
}

interface Props {
  tabs: PaneTab[];
  right?: boolean;
  left?: boolean;
  className?: string;
  dataTestId: string;
  beforeTabs?: ReactNode;
}

// every tab's content stays mounted while another tab shows, so editors and
// trees keep their state across a switch
const Pane = ({ tabs, left, className, dataTestId, beforeTabs }: Props) => {
  const { t } = useTranslation();
  const paneType = left ? "left" : "right";
  const activeTab = useSelector<StateT, string>(
    (state) => state.panes[paneType].activeTab,
  );
  const dispatch = useDispatch();

  return (
    <div className={root({ className })}>
      <Tabs
        selectedKey={activeTab}
        onSelectionChange={(tab) => dispatch(clickPaneTab({ paneType, tab }))}
      >
        <div className="grid grid-cols-[auto_minmax(0,1fr)] bg-white">
          {beforeTabs && <div className={beforeTabsBox()}>{beforeTabs}</div>}
          <TabList
            aria-label={left ? t("leftPane.tabs") : t("rightPane.tabs")}
            data-test-id={dataTestId}
          >
            {tabs.map(({ key, label, tooltip }) => (
              <Tab key={key} id={key} tooltip={tooltip}>
                {label}
              </Tab>
            ))}
          </TabList>
        </div>
        <div className={container()} data-test-id={`${dataTestId}-container`}>
          {tabs.map(({ key, content }) => (
            <TabPanel key={key} id={key} shouldForceMount>
              {content}
            </TabPanel>
          ))}
        </div>
      </Tabs>
    </div>
  );
};

export default Pane;
