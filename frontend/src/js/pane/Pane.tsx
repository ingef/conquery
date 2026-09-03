import { tv } from "tailwind-variants";

import PaneTabNavigation from "./PaneTabNavigation";
import type { TabNavigationTab } from "./TabNavigation";

const root = tv({ base: ["h-full w-full", "pt-[40px]"] });

const container = tv({
  base: ["relative", "flex flex-col", "h-full", "overflow-hidden"],
});

interface Props {
  tabs: TabNavigationTab[];
  right?: boolean;
  left?: boolean;
  className?: string;
  dataTestId: string;
  children: React.ReactNode;
}

const Pane = ({ tabs, left, children, className, dataTestId }: Props) => {
  const paneType = left ? "left" : "right";

  return (
    <div className={root({ className })}>
      <div className={container()}>
        <PaneTabNavigation
          tabs={tabs}
          paneType={paneType}
          dataTestId={dataTestId}
        />
        <div className={container()} data-test-id={`${dataTestId}-container`}>
          {children}
        </div>
      </div>
    </div>
  );
};

export default Pane;
