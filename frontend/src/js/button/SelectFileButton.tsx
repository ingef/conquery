import type { Ref } from "react";

import { tv } from "tailwind-variants";

import BasicButton, { type BasicButtonProps } from "./BasicButton";

const selectFileButton = tv({
  base: [
    "flex items-center",
    "gap-[5px]",
    "bg-transparent",
    "border-0",
    "text-[11px]",
    "text-gray-500",
    "font-light",
    "hover:underline",
  ],
});

export const SelectFileButton = ({
  className,
  ...props
}: BasicButtonProps & { ref?: Ref<HTMLButtonElement> }) => (
  <BasicButton className={selectFileButton({ className })} {...props} />
);
