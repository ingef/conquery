import { createContext, type ReactNode, useContext } from "react";
import {
  ToggleButtonGroup as RacToggleButtonGroup,
  type ToggleButtonGroupProps as RacToggleButtonGroupProps,
} from "react-aria-components";
import { tv } from "tailwind-variants";

const groupStyle = tv({
  base: "flex items-center gap-1",
  variants: {
    orientation: {
      horizontal: "flex-row",
      vertical: "flex-col",
    },
    // a group that grows with its data continues on the next line
    wrap: { true: "flex-wrap" },
  },
});

type Orientation = "horizontal" | "vertical";
type Size = "sm" | "md" | "lg";

/** what a ToggleButton needs to know about the group it sits in */
const ToggleButtonGroupContext = createContext<{ size?: Size } | null>(null);

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
 * A group of ToggleButtons with one selection, react-aria's ToggleButtonGroup
 * underneath: `selectionMode` single or multiple, `selectedKeys` /
 * `onSelectionChange` (or `defaultSelectedKeys`), `disallowEmptySelection`,
 * and the arrow keys move along the group. Each button carries its key as `id`.
 *
 *   <ToggleButtonGroup size="sm" selectionMode="single" disallowEmptySelection
 *     selectedKeys={[mode]} onSelectionChange={(keys) => …}>
 *     <ToggleButton id="range">Range</ToggleButton>
 *     <ToggleButton id="exact">Exact</ToggleButton>
 *   </ToggleButtonGroup>
 *
 * The buttons keep their own look, a small gap between them; the group stays
 * on one line unless it may `wrap`. Layout around the group belongs to the parent.
 */
export const ToggleButtonGroup = ({
  orientation = "horizontal",
  wrap = false,
  size,
  children,
  ...props
}: ToggleButtonGroupProps) => (
  <ToggleButtonGroupContext.Provider value={{ size }}>
    <RacToggleButtonGroup
      className={groupStyle({ orientation, wrap })}
      orientation={orientation}
      {...props}
    >
      {children}
    </RacToggleButtonGroup>
  </ToggleButtonGroupContext.Provider>
);
