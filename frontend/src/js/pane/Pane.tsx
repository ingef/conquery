import { tv } from "tailwind-variants";

import PaneTabNavigation from "./PaneTabNavigation";
import type { TabNavigationTab } from "./TabNavigation";

// starts below the absolutely positioned header. Two rows: the tab bar and
// the content, whose track can shrink to zero, so the content never pushes
// the pane past its bottom
const root = tv({
  base: ["grid grid-rows-[auto_minmax(0,1fr)]", "h-full w-full", "pt-header"],
});

// a column for the children of the pane; the left pane stacks several
const container = tv({
  base: ["relative", "flex flex-col", "min-h-0", "overflow-hidden"],
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
      <PaneTabNavigation
        tabs={tabs}
        paneType={paneType}
        dataTestId={dataTestId}
      />
      <div className={container()} data-test-id={`${dataTestId}-container`}>
        {children}
      </div>
    </div>
  );
};

export default Pane;
