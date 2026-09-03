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

const button = tv({
  base: [
    "inline-flex items-center justify-center",
    "shrink-0",
    "rounded",
    "border",
    "leading-none font-normal whitespace-nowrap",
    "cursor-pointer",
    "transition-[color,background-color,border-color,opacity] duration-100",
    "disabled:cursor-not-allowed disabled:opacity-40",
    "aria-pressed:text-primary-500",
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
      danger: [
        "bg-transparent text-red border-red",
        "hover:bg-red hover:text-white",
      ],
      // reads as a text link, aligns with text: no padding, no box
      link: [
        "bg-transparent border-transparent px-0",
        "text-gray-500",
        "hover:text-gray-800 hover:underline",
      ],
    },
    // every size is a fixed height, so text and icon-only buttons line up
    size: {
      sm: "h-6 px-2 gap-[5px] text-xs",
      md: "h-[30px] px-[15px] gap-[10px] text-sm",
      lg: "h-9 px-[18px] gap-[10px] text-base",
    },
    // an icon-only button is a square
    iconOnly: { true: "px-0" },
  },
  compoundVariants: [
    { iconOnly: true, size: "sm", class: "w-6" },
    { iconOnly: true, size: "md", class: "w-[30px]" },
    { iconOnly: true, size: "lg", class: "w-9" },
  ],
  defaultVariants: { intent: "secondary", size: "md" },
});

export interface ButtonProps
  extends Omit<RacButtonProps, "className" | "style" | "children" | "onClick"> {
  /** what the button does in its context; the look follows */
  intent?: "primary" | "secondary" | "tertiary" | "danger" | "link";
  size?: "sm" | "md" | "lg";
  className?: string;
  style?: CSSProperties;
  children?: ReactNode;
  ref?: Ref<HTMLButtonElement>;
}

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
 * A pressed state (toggles) is `aria-pressed`, which colors the button.
 * `link` is for a button that reads as a text link.
 */
export const Button = ({
  intent,
  size,
  className,
  children,
  ...props
}: ButtonProps) => (
  <RacButton
    className={button({
      intent,
      size,
      iconOnly: isIconOnly(children),
      className,
    })}
    {...props}
  >
    {children}
  </RacButton>
);
