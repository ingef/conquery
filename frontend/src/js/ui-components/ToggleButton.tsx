import { Children, isValidElement, type ReactNode, type Ref } from "react";
import {
  ToggleButton as RacToggleButton,
  type ToggleButtonProps as RacToggleButtonProps,
} from "react-aria-components";
import { tv } from "tailwind-variants";

import { buttonStyle } from "./Button";
import { Icon } from "./Icon";

// while selected, the button is bold and shows its highlight color
const toggleStyle = tv({
  extend: buttonStyle,
  base: "data-selected:font-bold",
  variants: {
    highlight: {
      primary: "data-selected:text-primary-500",
      danger: "data-selected:text-red",
    },
  },
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
 * works inside a ToggleButtonGroup and as a tooltip trigger. Pressing may
 * flip the state or open an editor for it.
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
}: ToggleButtonProps) => (
  <RacToggleButton
    className={toggleStyle({
      intent,
      size,
      highlight,
      iconOnly: isIconOnly(children),
    })}
    {...props}
  >
    {children}
  </RacToggleButton>
);
