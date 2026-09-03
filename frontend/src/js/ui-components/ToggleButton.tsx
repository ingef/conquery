import { Children, isValidElement, type ReactNode, type Ref } from "react";
import {
  ToggleButton as RacToggleButton,
  type ToggleButtonProps as RacToggleButtonProps,
} from "react-aria-components";
import { tv } from "tailwind-variants";

import { buttonStyle } from "./Button";
import { Icon } from "./Icon";
import { useToggleButtonGroup } from "./ToggleButtonGroup";

// while selected, the button is bold and shows its highlight color
const toggleStyle = tv({
  extend: buttonStyle,
  base: "data-selected:font-bold",
  variants: {
    highlight: {
      primary: "data-selected:text-primary-500",
      danger: "data-selected:text-red",
    },
    // a segment of a segmented control: bordered, recessed while off, white
    // while on, joined to its neighbors along the group's orientation
    segmented: {
      true: [
        "rounded-none",
        "border-gray-500",
        "bg-gray-50 text-gray-500",
        "hover:bg-white",
        "data-selected:bg-white",
      ],
    },
    orientation: { horizontal: "", vertical: "" },
  },
  compoundVariants: [
    {
      segmented: true,
      orientation: "horizontal",
      class: "first:rounded-l last:rounded-r not-first:-ml-px",
    },
    {
      segmented: true,
      orientation: "vertical",
      class: "first:rounded-t last:rounded-b not-first:-mt-px",
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

const isIconOnly = (children: ReactNode) => {
  const items = Children.toArray(children);
  return (
    items.length > 0 &&
    items.every((child) => isValidElement(child) && child.type === Icon)
  );
};

/**
 * A button whose look reflects a state that is on or off, in Button's look.
 * react-aria's ToggleButton underneath: `isSelected` / `onChange`, and it
 * works as a tooltip trigger. Inside a ToggleButtonGroup it is keyed by `id`,
 * takes the group's size unless it has its own, and joins a segmented group
 * as one of its segments. Pressing may flip the state or open an editor for it.
 *
 *   <ToggleButton isSelected={pinned} onChange={setPinned} aria-label="Pin">
 *     <Icon icon={faThumbtack} />
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
        segmented: group?.segmented,
        orientation: group?.orientation,
      })}
      {...props}
    >
      {children}
    </RacToggleButton>
  );
};
