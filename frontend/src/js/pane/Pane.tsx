import type { ReactNode } from "react";
import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import { tv } from "tailwind-variants";

import type { StateT } from "../app/reducers";
import { Tab, TabList, TabPanel, Tabs } from "../ui-components/Tabs";
import { clickPaneTab } from "./actions";

const root = tv({ base: ["h-full w-full", "pt-[40px]"] });

const container = tv({
  base: ["relative", "flex flex-col", "h-full", "overflow-hidden"],
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
}

// every tab's content stays mounted while another tab shows, so editors and
// trees keep their state across a switch
const Pane = ({ tabs, left, className, dataTestId }: Props) => {
  const { t } = useTranslation();
  const paneType = left ? "left" : "right";
  const activeTab = useSelector<StateT, string | null>(
    (state) => state.panes[paneType].activeTab,
  );
  const dispatch = useDispatch();

  return (
    <div className={root({ className })}>
      <div className={container()}>
        <Tabs
          selectedKey={activeTab ?? undefined}
          onSelectionChange={(key) =>
            dispatch(clickPaneTab({ paneType, tab: String(key) }))
          }
        >
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
          <div className={container()} data-test-id={`${dataTestId}-container`}>
            {tabs.map(({ key, content }) => (
              <TabPanel key={key} id={key} shouldForceMount>
                {content}
              </TabPanel>
            ))}
          </div>
        </Tabs>
      </div>
    </div>
  );
};

export default Pane;
