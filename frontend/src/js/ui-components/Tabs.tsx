import {
  type ContextType,
  createContext,
  type ReactNode,
  type RefObject,
  useContext,
  useEffect,
  useRef,
} from "react";
import {
  Tab as RacTab,
  TabList as RacTabList,
  type TabListProps as RacTabListProps,
  TabPanel as RacTabPanel,
  type TabPanelProps as RacTabPanelProps,
  type TabProps as RacTabProps,
  Tabs as RacTabs,
  type TabsProps as RacTabsProps,
  TabListStateContext,
} from "react-aria-components";
import { tv } from "tailwind-variants";

import { useHoverNavigate } from "./HoverNavigatable";
import {
  Tooltip,
  TooltipTarget,
  TooltipTrigger,
  tooltipDelay,
} from "./Tooltip";

type Variant = "primary" | "secondary";

type TabListState = NonNullable<ContextType<typeof TabListStateContext>>;

// react-aria renders the tabs once into a hidden tree to collect them and once
// for real; a Tab's hooks run in the hidden pass, where react-aria's state
// context is not provided yet. This context sits above both passes and hands
// the Tab the real state through a ref.
const TabsContext = createContext<{
  variant: Variant;
  stateRef: RefObject<TabListState | null>;
}>({ variant: "primary", stateRef: { current: null } });

// rendered inside react-aria's Tabs, so the real pass sees the state
const StateBridge = ({
  stateRef,
}: {
  stateRef: RefObject<TabListState | null>;
}) => {
  const state = useContext(TabListStateContext);
  useEffect(() => {
    if (state) stateRef.current = state;
  }, [state, stateRef]);
  return null;
};

const root = tv({
  base: ["flex flex-col", "min-h-0"],
  variants: {
    // a main navigation fills its pane
    variant: { primary: "h-full", secondary: "" },
  },
});

const list = tv({
  base: "flex items-start",
  variants: {
    variant: {
      // a line the tabs sit on
      primary: ["px-5", "border-b border-gray-100", "bg-white"],
      secondary: "pt-[3px] pl-[10px]",
    },
  },
});

const tab = tv({
  base: [
    "relative",
    "flex items-center",
    "shrink-0 whitespace-nowrap",
    "cursor-pointer",
    "outline-none",
    "transition-colors duration-100",
    "data-focus-visible:outline-2 data-focus-visible:-outline-offset-2 data-focus-visible:outline-primary-500",
  ],
  variants: {
    variant: {
      primary: [
        "mt-[6px] mr-[5px] px-3",
        "border-b-[3px] border-transparent",
        "text-sm leading-[30px] font-bold uppercase tracking-wider",
        "text-gray-500",
        "not-data-selected:data-hovered:border-primary-200 not-data-selected:data-hovered:text-black",
        "data-selected:border-primary-500 data-selected:text-primary-500",
      ],
      // sits on the box below it like a folder tab
      secondary: [
        "mx-[2px] px-[10px]",
        "h-[30px]",
        "translate-y-px",
        "rounded-t",
        "border border-b-0 border-transparent",
        "text-sm",
        "text-gray-500",
        "not-data-selected:data-hovered:border-gray-400",
        "data-selected:border-gray-500 data-selected:bg-bg-50 data-selected:text-gray-800",
      ],
    },
    // a dragged item may be dropped here: the tab switches while hovering
    droppable: { true: "bg-gray-50" },
    over: { true: "bg-gray-100" },
  },
});

const tabTarget = tv({ base: "block" });

// a force-mounted panel of an unselected tab is inert: hidden, state kept
const panel = tv({
  base: [
    "flex flex-col",
    "grow",
    "min-h-0",
    "outline-none",
    "data-inert:hidden",
  ],
});

export interface TabsProps
  extends Omit<
    RacTabsProps,
    | "className"
    | "style"
    | "children"
    | "orientation"
    | "selectedKey"
    | "defaultSelectedKey"
    | "onSelectionChange"
  > {
  children: ReactNode;
  /** `primary` underlines the selected tab, `secondary` connects it to the box below */
  variant?: Variant;
  selectedKey?: string;
  defaultSelectedKey?: string;
  onSelectionChange?: (key: string) => void;
}

/**
 * Tabs on react-aria-components: `selectedKey` / `onSelectionChange`, arrow
 * keys move between the tabs, each keyed by `id`. The `TabPanel` with the
 * same id shows while its tab is selected; `shouldForceMount` keeps a panel
 * mounted (hidden and inert) so its state survives a switch. A dragged item
 * hovering over a tab switches to it.
 *
 *   <Tabs selectedKey={tab} onSelectionChange={setTab}>
 *     <TabList aria-label="Editors">
 *       <Tab id="query" tooltip="…">Editor</Tab>
 *       <Tab id="forms">Form Editor</Tab>
 *     </TabList>
 *     <TabPanel id="query">…</TabPanel>
 *   </Tabs>
 */
export const Tabs = ({
  variant = "primary",
  children,
  onSelectionChange,
  ...props
}: TabsProps) => {
  const stateRef = useRef<TabListState | null>(null);
  return (
    <TabsContext.Provider value={{ variant, stateRef }}>
      <RacTabs
        className={root({ variant })}
        // the ids are strings, react-aria's key type also allows numbers
        onSelectionChange={(key) => onSelectionChange?.(String(key))}
        {...props}
      >
        <StateBridge stateRef={stateRef} />
        {children}
      </RacTabs>
    </TabsContext.Provider>
  );
};

export interface TabListProps
  extends Omit<
    RacTabListProps<object>,
    "className" | "style" | "children" | "items"
  > {
  "aria-label": string;
  children: ReactNode;
  "data-test-id"?: string;
}

export const TabList = ({ children, ...props }: TabListProps) => {
  const { variant } = useContext(TabsContext);
  return (
    <RacTabList className={list({ variant })} {...props}>
      {children}
    </RacTabList>
  );
};

export interface TabProps
  extends Omit<RacTabProps, "className" | "style" | "children"> {
  id: string;
  children: ReactNode;
  /** further information, shown after a while */
  tooltip?: string;
}

export const Tab = ({ id, tooltip, children, ...props }: TabProps) => {
  const { variant, stateRef } = useContext(TabsContext);
  const { drop, isOver, isDroppable } = useHoverNavigate({
    triggerNavigate: () => {
      const state = stateRef.current;
      if (state && state.selectedKey !== id) state.setSelectedKey(id);
    },
  });

  return (
    <RacTab
      id={id}
      ref={(el) => {
        drop(el);
      }}
      className={tab({
        variant,
        droppable: isDroppable,
        over: isOver && isDroppable,
      })}
      {...props}
    >
      {tooltip ? (
        <TooltipTrigger delay={tooltipDelay.long}>
          <TooltipTarget as="span" excludeFromTabOrder className={tabTarget()}>
            {children}
          </TooltipTarget>
          <Tooltip>{tooltip}</Tooltip>
        </TooltipTrigger>
      ) : (
        children
      )}
    </RacTab>
  );
};

export interface TabPanelProps
  extends Omit<RacTabPanelProps, "className" | "style" | "children"> {
  id: string;
  children: ReactNode;
}

export const TabPanel = ({ children, ...props }: TabPanelProps) => (
  <RacTabPanel className={panel()} {...props}>
    {children}
  </RacTabPanel>
);
