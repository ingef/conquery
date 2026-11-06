import { createContext, type ReactNode, useContext } from "react";
import {
  ToggleButtonGroup as RacToggleButtonGroup,
  type ToggleButtonGroupProps as RacToggleButtonGroupProps,
} from "react-aria-components";
import { tv } from "tailwind-variants";

const groupStyle = tv({
  base: "flex items-center",
  variants: {
    orientation: {
      horizontal: "flex-row",
      vertical: "flex-col",
    },
    // a group that grows with its data continues on the next line
    wrap: { true: "flex-wrap" },
    // joined buttons overlap their borders by a pixel; the padding gives the
    // first button's border its room
    segmented: {
      true: "pt-px pl-px",
      false: "gap-1",
    },
  },
});

type Orientation = "horizontal" | "vertical";
type Size = "sm" | "md" | "lg";

/** what a ToggleButton needs to know about the group it sits in */
const ToggleButtonGroupContext = createContext<{
  size?: Size;
  segmented: boolean;
  orientation: Orientation;
  wrap: boolean;
} | null>(null);

export const useToggleButtonGroup = () => useContext(ToggleButtonGroupContext);

export interface ToggleButtonGroupProps
  extends Omit<RacToggleButtonGroupProps, "className" | "style" | "children"> {
  children?: ReactNode;
  orientation?: Orientation;
  /** the buttons may continue on further lines: for a group whose options come from data */
  wrap?: boolean;
  /** the size of every button in the group */
  size?: Size;
}

/**
 * A group of ToggleButtons, react-aria's ToggleButtonGroup underneath:
 * `selectionMode` single or multiple, `selectedKeys` / `onSelectionChange`
 * (or `defaultSelectedKeys`), `disallowEmptySelection`, and the arrow keys
 * move along the group. Each button carries its key as `id`.
 *
 * A group with `selectionMode="single"` is one choice among options, a radio
 * group to assistive technology, and looks like one control: the buttons join
 * into a segmented bar and the chosen one is filled. A group with multiple
 * selection is a row of independent switches, each in its own button look.
 *
 *   <ToggleButtonGroup selectionMode="single" disallowEmptySelection
 *     selectedKeys={[mode]} onSelectionChange={(keys) => …}>
 *     <ToggleButton id="range">Range</ToggleButton>
 *     <ToggleButton id="exact">Exact</ToggleButton>
 *   </ToggleButtonGroup>
 *
 * The group stays on one line unless it may `wrap`. Layout around the group
 * belongs to the parent.
 */
export const ToggleButtonGroup = ({
  orientation = "horizontal",
  wrap = false,
  size,
  selectionMode,
  children,
  ...props
}: ToggleButtonGroupProps) => {
  const segmented = selectionMode === "single";
  return (
    <ToggleButtonGroupContext.Provider
      value={{ size, segmented, orientation, wrap }}
    >
      <RacToggleButtonGroup
        className={groupStyle({ orientation, wrap, segmented })}
        orientation={orientation}
        selectionMode={selectionMode}
        {...props}
      >
        {children}
      </RacToggleButtonGroup>
    </ToggleButtonGroupContext.Provider>
  );
};
