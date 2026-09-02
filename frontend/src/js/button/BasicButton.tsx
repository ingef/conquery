import type { ButtonHTMLAttributes, Ref } from "react";
import { mergeProps, useFocusable, useObjectRef } from "react-aria";
import { tv } from "tailwind-variants";

export interface BasicButtonProps
  extends ButtonHTMLAttributes<HTMLButtonElement> {
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
}: BasicButtonProps & { ref?: Ref<HTMLButtonElement> }) => {
  const domRef = useObjectRef(ref);
  // A surrounding TooltipTrigger hands its hover/focus props to the
  // nearest focusable element: this makes every button a tooltip trigger.
  const { focusableProps } = useFocusable({ isDisabled: disabled }, domRef);

  return (
    <button
      type="button"
      className={button({
        bare,
        tiny,
        small,
        large,
        active,
        secondary,
        className,
      })}
      disabled={disabled}
      {...mergeProps(focusableProps, props)}
      ref={domRef}
    />
  );
};

export default BasicButton;
