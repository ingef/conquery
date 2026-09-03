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
      horizontal: "flex-row flex-wrap gap-1",
      vertical: "flex-col gap-1",
    },
    // the buttons join into one control and share their borders
    segmented: { true: "inline-flex flex-nowrap gap-0" },
  },
  defaultVariants: { orientation: "horizontal" },
});

type Orientation = "horizontal" | "vertical";
type Size = "sm" | "md" | "lg";

/** what a ToggleButton needs to know about the group it sits in */
export const ToggleButtonGroupContext = createContext<{
  segmented: boolean;
  orientation: Orientation;
  size?: Size;
} | null>(null);

export const useToggleButtonGroup = () => useContext(ToggleButtonGroupContext);

export interface ToggleButtonGroupProps
  extends Omit<RacToggleButtonGroupProps, "className" | "style" | "children"> {
  children?: ReactNode;
  orientation?: Orientation;
  /** one connected control: the buttons share their borders, a segmented control */
  segmented?: boolean;
  /** the size of every button in the group */
  size?: Size;
}

/**
 * A group of ToggleButtons with one selection, react-aria's ToggleButtonGroup
 * underneath: `selectionMode` single or multiple, `selectedKeys` /
 * `onSelectionChange` (or `defaultSelectedKeys`), `disallowEmptySelection`,
 * and the arrow keys move along the group. Each button carries its key as `id`.
 *
 *   <ToggleButtonGroup segmented size="sm" selectionMode="single" disallowEmptySelection
 *     selectedKeys={[mode]} onSelectionChange={(keys) => …}>
 *     <ToggleButton id="range">Range</ToggleButton>
 *     <ToggleButton id="exact">Exact</ToggleButton>
 *   </ToggleButtonGroup>
 *
 * Plain, the buttons keep their own look with a small gap; `segmented` joins
 * them into one bordered control. Layout around the group belongs to the parent.
 */
export const ToggleButtonGroup = ({
  orientation = "horizontal",
  segmented = false,
  size,
  children,
  ...props
}: ToggleButtonGroupProps) => (
  <ToggleButtonGroupContext.Provider value={{ segmented, orientation, size }}>
    <RacToggleButtonGroup
      className={groupStyle({ orientation, segmented })}
      orientation={orientation}
      {...props}
    >
      {children}
    </RacToggleButtonGroup>
  </ToggleButtonGroupContext.Provider>
);
