import type { ComponentProps } from "react";
import { tv } from "tailwind-variants";

const label = tv({
  base: [
    "flex items-center",
    "mx-0 mt-[6px] mb-[3px]",
    "w-[inherit]",
    "text-sm",
    "font-normal",
    "text-gray-800",
  ],
  variants: {
    // later wins when both are set
    tiny: { true: "text-xs" },
    large: { true: "text-base" },
    disabled: { true: "text-gray-500" },
    fullWidth: { true: "w-full" },
  },
});

const Label = ({
  className,
  tiny,
  large,
  disabled,
  fullWidth,
  ...props
}: ComponentProps<"span"> & {
  tiny?: boolean;
  large?: boolean;
  disabled?: boolean;
  fullWidth?: boolean;
}) => (
  <span
    className={label({ tiny, large, disabled, fullWidth, className })}
    {...props}
  />
);

export default Label;
