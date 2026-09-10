import { Children, isValidElement, type ReactNode, type Ref } from "react";
import {
  ToggleButton as RacToggleButton,
  type ToggleButtonProps as RacToggleButtonProps,
} from "react-aria-components";
import { tv } from "tailwind-variants";

import { buttonStyle } from "./Button";
import { Icon } from "./Icon";
import { useToggleButtonGroup } from "./ToggleButtonGroup";

// quiet while off, so the selected state stands out: gray text that darkens
// on hover; while selected, bold in the highlight color
const toggleStyle = tv({
  extend: buttonStyle,
  base: "data-selected:font-bold",
  variants: {
    intent: {
      secondary: "text-gray-500 not-data-selected:hover:text-gray-800",
      tertiary: "text-gray-500 not-data-selected:hover:text-gray-800",
    },
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
 * works as a tooltip trigger. Inside a ToggleButtonGroup it is keyed by `id`
 * and takes the group's size unless it has its own. Pressing may flip the
 * state or open an editor for it.
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
      })}
      {...props}
    >
      {children}
    </RacToggleButton>
  );
};
