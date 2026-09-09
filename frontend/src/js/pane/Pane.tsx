import { useTranslation } from "react-i18next";
import { useDispatch, useSelector } from "react-redux";
import { tv } from "tailwind-variants";

import type { StateT } from "../app/reducers";
import { Tab, TabList, Tabs } from "../ui-components/Tabs";
import { clickPaneTab } from "./actions";

const root = tv({ base: ["h-full w-full", "pt-[40px]"] });

const container = tv({
  base: ["relative", "flex flex-col", "h-full", "overflow-hidden"],
});

export interface PaneTab {
  key: string;
  label: string;
  tooltip?: string;
}

interface Props {
  tabs: PaneTab[];
  right?: boolean;
  left?: boolean;
  className?: string;
  dataTestId: string;
  children: React.ReactNode;
}

const Pane = ({ tabs, left, children, className, dataTestId }: Props) => {
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
        </Tabs>
        <div className={container()} data-test-id={`${dataTestId}-container`}>
          {children}
        </div>
      </div>
    </div>
  );
};

export default Pane;
