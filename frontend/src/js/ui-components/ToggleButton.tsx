import { Children, isValidElement, type ReactNode, type Ref } from "react";
import {
  ToggleButton as RacToggleButton,
  type ToggleButtonProps as RacToggleButtonProps,
} from "react-aria-components";
import { tv } from "tailwind-variants";

import { buttonStyle } from "./Button";
import { Icon } from "./Icon";

// selected: the primary color, or red for a warning toggle
const toggleStyle = tv({
  extend: buttonStyle,
  variants: {
    danger: {
      true: "data-selected:text-red",
      false: "data-selected:text-primary-500",
    },
  },
});

interface CommonProps
  extends Omit<RacToggleButtonProps, "className" | "style" | "children"> {
  size?: "sm" | "md" | "lg";
  children?: ReactNode;
  ref?: Ref<HTMLButtonElement>;
}

export type ToggleButtonProps = CommonProps & {
  intent?: "secondary" | "tertiary";
  /** a warning state: red while selected */
  danger?: boolean;
};

const isIconOnly = (children: ReactNode) => {
  const items = Children.toArray(children);
  return (
    items.length > 0 &&
    items.every((child) => isValidElement(child) && child.type === Icon)
  );
};

/**
 * A button that is on or off, in Button's look. react-aria's ToggleButton
 * underneath: `isSelected` / `onChange`, and it works inside a
 * ToggleButtonGroup and as a tooltip trigger.
 *
 *   <ToggleButton isSelected={pinned} onChange={setPinned} aria-label="Pin">
 *     <Icon icon={faThumbtack} />
 *   </ToggleButton>
 */
export const ToggleButton = ({
  intent = "tertiary",
  size,
  danger = false,
  children,
  ...props
}: ToggleButtonProps) => (
  <RacToggleButton
    className={toggleStyle({
      intent,
      size,
      danger,
      iconOnly: isIconOnly(children),
    })}
    {...props}
  >
    {children}
  </RacToggleButton>
);
