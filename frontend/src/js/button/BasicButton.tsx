import type { CSSProperties, ReactNode, Ref } from "react";
import {
  Button as RacButton,
  type ButtonProps as RacButtonProps,
} from "react-aria-components";
import { tv } from "tailwind-variants";

export interface BasicButtonProps
  extends Omit<
    RacButtonProps,
    "className" | "style" | "children" | "isDisabled"
  > {
  className?: string;
  style?: CSSProperties;
  children?: ReactNode;
  /** maps to react-aria's `isDisabled` */
  disabled?: boolean;
  bare?: boolean;
  tiny?: boolean;
  small?: boolean;
  large?: boolean;
  active?: boolean;
  secondary?: boolean;
}

const button = tv({
  base: [
    "cursor-pointer",
    "rounded",
    "px-[15px] py-2",
    "text-sm",
    "font-normal",
    "transition-all duration-200",
    "disabled:cursor-not-allowed disabled:opacity-40",
  ],
  variants: {
    active: { true: "font-bold" },
    secondary: { true: "font-bold" },
    // later wins when several are set
    large: { true: "px-[18px] py-3 text-xl" },
    small: { true: "px-2 py-[6px] text-xs" },
    tiny: { true: "px-[6px] py-1 text-xs" },
    bare: { true: "p-0" },
  },
});

// react-aria's Button is the trigger that TooltipTrigger, MenuTrigger and
// friends expect. `onClick` is react-aria's alias for `onPress` and receives
// a mouse event; react-aria prefers `onPress`.
const BasicButton = ({
  ref,
  className,
  bare,
  tiny,
  small,
  large,
  active,
  secondary,
  disabled,
  ...props
}: BasicButtonProps & { ref?: Ref<HTMLButtonElement> }) => (
  <RacButton
    className={button({
      bare,
      tiny,
      small,
      large,
      active,
      secondary,
      className,
    })}
    isDisabled={disabled}
    {...props}
    ref={ref}
  />
);

export default BasicButton;
