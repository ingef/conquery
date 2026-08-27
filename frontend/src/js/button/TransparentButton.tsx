import type { Ref } from "react";

import { tv } from "tailwind-variants";

import BasicButton, { type BasicButtonProps } from "./BasicButton";

const transparentButton = tv({
  base: [
    "rounded",
    "bg-transparent hover:bg-gray-50 focus:bg-gray-50",
    "border border-gray-500 focus:border-green",
    "text-gray-800",
  ],
  variants: {
    light: { true: "border-gray-100 text-gray-500" },
  },
});

export const TransparentButton = ({
  className,
  light,
  ...props
}: BasicButtonProps & { light?: boolean; ref?: Ref<HTMLButtonElement> }) => (
  <BasicButton className={transparentButton({ light, className })} {...props} />
);
