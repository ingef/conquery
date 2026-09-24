import type { ReactNode, Ref } from "react";
import {
  ToggleButton as RacToggleButton,
  type ToggleButtonProps as RacToggleButtonProps,
} from "react-aria-components";
import { tv } from "tailwind-variants";

import { buttonStyle, isIconOnly } from "./Button";
import { useToggleButtonGroup } from "./ToggleButtonGroup";

// quiet while off, so the selected state stands out: gray text that darkens
// on hover; while selected, the highlight color
const toggleStyle = tv({
  extend: buttonStyle,
  variants: {
    intent: {
      secondary: "text-gray-600 not-data-selected:hover:text-gray-800",
      tertiary: "text-gray-600 not-data-selected:hover:text-gray-800",
    },
    highlight: {
      primary: "data-selected:text-primary-500",
      danger: "data-selected:text-red",
    },
    // a segment of a single-selection group: the frame of a secondary button
    // shared with its neighbors, filled like a primary button while selected
    segmented: {
      true: [
        "rounded-none",
        "border-gray-500",
        "-mt-px -ml-px",
        "data-selected:relative data-selected:z-10",
        "data-selected:bg-primary-500 data-selected:border-primary-500 data-selected:text-white",
        "data-selected:hover:bg-primary-500",
      ],
    },
    orientation: { horizontal: "", vertical: "" },
    wrap: { true: "", false: "" },
  },
  compoundVariants: [
    {
      segmented: true,
      highlight: "danger",
      class:
        "data-selected:bg-red data-selected:border-red data-selected:hover:bg-red",
    },
    // the outer corners of the bar; a wrapped bar has them at its top left and bottom right
    {
      segmented: true,
      orientation: "horizontal",
      wrap: false,
      class: "first:rounded-l last:rounded-r",
    },
    {
      segmented: true,
      orientation: "horizontal",
      wrap: true,
      class: "first:rounded-tl last:rounded-br",
    },
    {
      segmented: true,
      orientation: "vertical",
      class: "first:rounded-t last:rounded-b",
    },
  ],
});

interface CommonProps
  extends Omit<RacToggleButtonProps, "className" | "style" | "children"> {
  size?: "sm" | "md" | "lg";
  children?: ReactNode;
  ref?: Ref<HTMLButtonElement>;
}

export interface ToggleButtonProps extends CommonProps {
  /** Button's intents that can be switched on and off */
  intent?: "secondary" | "tertiary";
  /** how it shows while selected: the primary color, or red for a warning state */
  highlight?: "primary" | "danger";
}

/**
 * A button whose look reflects a state that is on or off, in Button's look.
 * react-aria's ToggleButton underneath: `isSelected` / `onChange`, and it
 * works as a tooltip trigger. Inside a ToggleButtonGroup it is keyed by `id`
 * and takes the group's size unless it has its own; in a single-selection
 * group it is a segment of one bar. Pressing may flip the state or open an
 * editor for it.
 *
 *   <ToggleButton isSelected={pinned} onChange={setPinned} aria-label="Pin">
 *     <PinIcon />
 *   </ToggleButton>
 */
export const ToggleButton = ({
  intent = "tertiary",
  size,
  highlight = "primary",
  children,
  ...props
}: ToggleButtonProps) => {
  const group = useToggleButtonGroup();
  return (
    <RacToggleButton
      className={toggleStyle({
        intent,
        size: size ?? group?.size,
        highlight,
        iconOnly: isIconOnly(children),
        segmented: group?.segmented ?? false,
        orientation: group?.orientation,
        wrap: group?.wrap,
      })}
      {...props}
    >
      {children}
    </RacToggleButton>
  );
};
