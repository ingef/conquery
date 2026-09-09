import { createContext, type ReactNode, useContext } from "react";
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

type Variant = "underline" | "boxed";
type Size = "sm" | "md";

const TabsStyleContext = createContext<{ variant: Variant; size: Size }>({
  variant: "underline",
  size: "md",
});

const list = tv({
  base: "flex items-start",
  variants: {
    variant: { underline: "", boxed: "pt-[3px] pl-[10px]" },
    size: { sm: "", md: "" },
  },
  compoundVariants: [
    // the panes' main navigation: a line the tabs sit on
    {
      variant: "underline",
      size: "md",
      class: ["px-5", "border-b border-gray-100", "bg-white"],
    },
  ],
});

const tab = tv({
  base: [
    "relative",
    "cursor-pointer",
    "outline-none",
    "transition-colors duration-100",
    "data-focus-visible:outline-2 data-focus-visible:-outline-offset-2 data-focus-visible:outline-primary-500",
  ],
  variants: {
    variant: { underline: "", boxed: "" },
    size: { sm: "", md: "" },
    // a dragged item may be dropped here: the tab switches while hovering
    droppable: { true: "bg-gray-50" },
    over: { true: "bg-gray-100" },
  },
  compoundVariants: [
    {
      variant: "underline",
      size: "md",
      class: [
        "mt-[6px] mr-[5px] px-3",
        "border-b-[3px] border-transparent",
        "text-sm leading-[30px] font-bold uppercase tracking-wider",
        "text-gray-500",
        "not-data-selected:data-hovered:border-primary-200 not-data-selected:data-hovered:text-black",
        "data-selected:border-primary-500 data-selected:text-primary-500",
      ],
    },
    {
      variant: "underline",
      size: "sm",
      class: [
        "mx-[2px] px-[3px]",
        "h-[26px]",
        "rounded-t",
        "text-xs uppercase",
        "text-gray-500",
        "after:absolute after:inset-x-0 after:bottom-0 after:h-[3px] after:content-['']",
        "not-data-selected:data-hovered:after:bg-gray-100",
        "data-selected:text-gray-800 data-selected:after:bg-gray-800",
      ],
    },
    // sits on the box below it like a folder tab
    {
      variant: "boxed",
      size: "md",
      class: [
        "mx-[2px] px-[10px]",
        "h-[30px] leading-[28px]",
        "translate-y-px",
        "rounded-t",
        "border border-b-0 border-transparent",
        "text-sm",
        "text-gray-500",
        "not-data-selected:data-hovered:border-gray-400",
        "data-selected:border-gray-500 data-selected:bg-bg-50 data-selected:text-gray-800",
      ],
    },
  ],
});

const tabTarget = tv({ base: "block" });

export interface TabsProps
  extends Omit<
    RacTabsProps,
    "className" | "style" | "children" | "orientation"
  > {
  children: ReactNode;
  /** `underline` marks the selected tab with a bar, `boxed` connects it to the box below */
  variant?: Variant;
  /** `md` for a main navigation, `sm` for a switch inside a pane */
  size?: Size;
}

/**
 * Tabs on react-aria-components: `selectedKey` / `onSelectionChange`, arrow
 * keys move between the tabs, each keyed by `id`. A `TabPanel` with the same
 * id shows while its tab is selected; content rendered elsewhere works too.
 * A dragged item hovering over a tab switches to it.
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
  variant = "underline",
  size = "md",
  children,
  ...props
}: TabsProps) => (
  <TabsStyleContext.Provider value={{ variant, size }}>
    <RacTabs {...props}>{children}</RacTabs>
  </TabsStyleContext.Provider>
);

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
  const { variant, size } = useContext(TabsStyleContext);
  return (
    <RacTabList className={list({ variant, size })} {...props}>
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
  const { variant, size } = useContext(TabsStyleContext);
  const state = useContext(TabListStateContext);
  const { drop, isOver, isDroppable } = useHoverNavigate({
    triggerNavigate: () => {
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
        size,
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
  <RacTabPanel {...props}>{children}</RacTabPanel>
);
