import {
  Children,
  type CSSProperties,
  isValidElement,
  type ReactNode,
  type Ref,
} from "react";
import {
  Button as RacButton,
  type ButtonProps as RacButtonProps,
} from "react-aria-components";
import { tv } from "tailwind-variants";

import { Icon } from "./Icon";

export const buttonStyle = tv({
  base: [
    "inline-flex items-center justify-center",
    "shrink-0",
    "rounded",
    "border",
    "leading-none font-medium whitespace-nowrap",
    "cursor-pointer",
    "transition-[color,background-color,border-color,opacity] duration-100",
    "disabled:cursor-not-allowed disabled:opacity-40",
  ],
  variants: {
    intent: {
      primary: [
        "bg-primary-500 text-white border-primary-500",
        "hover:opacity-90",
      ],
      secondary: [
        "bg-transparent text-gray-800 border-gray-500",
        "hover:bg-gray-50",
      ],
      tertiary: [
        "bg-transparent text-gray-800 border-transparent",
        "hover:bg-gray-50",
      ],
      // reads as a text link, see the compound variant for how it flows with text
      link: [
        "bg-transparent border-transparent",
        "text-gray-500",
        "hover:text-gray-800 hover:underline",
      ],
    },
    // every size is a fixed height, so text and icon-only buttons line up
    size: {
      sm: "h-6 px-2 gap-2 text-xs",
      md: "h-[30px] px-[15px] gap-2 text-sm",
      lg: "h-9 px-[18px] gap-3 text-base",
    },
    // an icon-only button is a square
    iconOnly: { true: "px-0" },
    // a destructive or warning action, in the look of its intent
    danger: { true: "" },
  },
  compoundVariants: [
    // a link sits in flowing text: no box, no padding, no fixed height,
    // the surrounding text's size and line-height (after the size variant)
    {
      intent: "link",
      class:
        "h-auto px-0 gap-1 leading-[inherit] text-[length:inherit] align-baseline",
    },
    {
      danger: true,
      intent: "secondary",
      class: "text-red border-red hover:bg-red hover:text-white",
    },
    { danger: true, intent: "tertiary", class: "text-red hover:text-red" },
    { iconOnly: true, size: "sm", class: "w-6" },
    { iconOnly: true, size: "md", class: "w-[30px]" },
    { iconOnly: true, size: "lg", class: "w-9" },
  ],
  defaultVariants: { intent: "secondary", size: "md" },
});

interface CommonProps
  extends Omit<RacButtonProps, "className" | "style" | "children" | "onClick"> {
  size?: "sm" | "md" | "lg";
  style?: CSSProperties;
  children?: ReactNode;
  ref?: Ref<HTMLButtonElement>;
}

/** what the button does in its context; the look follows */
export type ButtonProps = CommonProps &
  (
    | {
        intent?: "secondary" | "tertiary";
        /** a destructive or warning action: red, in the look of the intent */
        danger?: boolean;
      }
    | { intent: "primary" | "link"; danger?: never }
  );

const isIconOnly = (children: ReactNode) => {
  const items = Children.toArray(children);
  return (
    items.length > 0 &&
    items.every((child) => isValidElement(child) && child.type === Icon)
  );
};

/**
 * The button. react-aria's Button underneath, so it is the trigger that
 * TooltipTrigger, MenuTrigger and friends expect, handles press via
 * `onPress`, and exposes `data-hovered`, `data-pressed`, `data-focus-visible`.
 *
 * An icon goes in as a child and takes the button's text color:
 *
 *   <Button intent="primary" onPress={save}><Icon icon={faCheck} />Save</Button>
 *   <Button intent="tertiary" aria-label="Delete"><Icon icon={faTrash} /></Button>
 *
 * A button whose only children are icons is square; give it an `aria-label`.
 * `link` is for a button that reads as a text link and sits in flowing text.
 * `danger` turns a secondary or tertiary button red for destructive or
 * warning actions. Something that is on or off is a ToggleButton.
 *
 * There is no className: layout belongs to the parent (a `grid` wrapper
 * stretches a button to full width), and anything that needs another look is
 * not this button but a react-aria Button styled where it lives.
 */
export const Button = ({
  intent,
  size,
  danger,
  children,
  ...props
}: ButtonProps) => (
  <RacButton
    className={buttonStyle({
      intent,
      size,
      danger,
      iconOnly: isIconOnly(children),
    })}
    {...props}
  >
    {children}
  </RacButton>
);
